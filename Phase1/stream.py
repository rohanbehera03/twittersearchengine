from threading import Thread
import time
import tweepy
from dotenv import dotenv_values
import json
from typing import List, Dict
from os.path import exists as file_exists, getsize as file_size

# Globals
file_num: int = 0
streamed_tweets: List[Dict] = []
total_tweets = 0
stream_rules: List[Dict] = None

# Load .env file
config = dotenv_values(".env")
bearer_token = config["BEARER_TOKEN"]
FILE_PREFIX = config["FILE_PREFIX"]
TWEETS_IN_FILE = int(config["TWEETS_IN_FILE"])

# Check if file with prefix exists to prevent overwriting
if file_exists(f"{FILE_PREFIX}{file_num}.json"):
    print(f"ERROR: Files with the prefix '{FILE_PREFIX}' already exist and would be overwritten! " + 
          "Please move or delete those files and re-run the program!")
    exit(1)

# Load stream rules
with open("stream_rules.json") as f:
    stream_rules = json.load(f)

# Validate stream rules
def filter_stream_rules(stream_rule: Dict) -> bool:
    tag: str = stream_rule["tag"]
    rule: str = stream_rule["rule"]

    if (len(rule) > 512):
        print(f"Error stream rule with tag '{tag}' is greater than 512 characters!")
        return False

    return True

stream_rules = filter(filter_stream_rules, stream_rules)

# Convert a tweet to a python dictionary based on the relevant fields
def tweet_to_dict(tweet) -> dict:
    tweet_dict = {}
    tweet_dict["id"] = tweet.id
    tweet_dict["user_id"] = tweet.author_id
    tweet_dict["created_at"] = tweet.created_at
    tweet_dict["device"] = tweet.source

    if tweet.geo is not None and "coordinates" in tweet.geo:
        coords = tweet.geo["coordinates"]
        if "coordinates" in coords:
            point_coords = coords["coordinates"]
            tweet_dict["location"] = point_coords

    tweet_dict["text"] = tweet.text

    if tweet.public_metrics is not None:
        metrics = tweet.public_metrics
        if "like_count" in metrics:
            tweet_dict["likes"] = metrics["like_count"]
        
        if "retweet_count" in metrics:
            tweet_dict["retweets"] = metrics["retweet_count"]
    
    return tweet_dict

# Filter tweets excluding all tweets that are re-tweets
def filter_tweet(tweet: object) -> bool:
    if tweet.text.startswith("RT @"):
        return False

    return True

# Store the tweets via async I/O
def store_tweets(tweets):
    global file_num
    old_file_num = file_num
    file_num = file_num + 1

    def store_tweets_threaded(tweets, file_name):
        # Write the filtered tweets to JSON
        with open(file_name, 'w') as f:
            f.write('[')
            tweets_json = [ json.dumps(tweet, indent=4, default=str) for tweet in tweets ]
            f.write(",\n".join(tweets_json))
            f.write("]")

    file_name = f"{FILE_PREFIX}{old_file_num}.json"
    thread = Thread(target = store_tweets_threaded, args = (tweets, file_name,))
    thread.start()

# Sub-class the streaming client
class TweetStorer(tweepy.StreamingClient):

    def on_tweet(self, tweet):
        # Check if tweet is something we want to keep
        if filter_tweet(tweet):
            global streamed_tweets
            global total_tweets

            # Add it to the list of streamed tweets
            streamed_tweets.append(tweet_to_dict(tweet))
            total_tweets = total_tweets + 1
    
            num_streamed_tweets = len(streamed_tweets)
            # If we've reached the number of streamed tweets, then store the tweets and clear stream tweets.
            if num_streamed_tweets >= TWEETS_IN_FILE:
                stored_tweets = streamed_tweets
                streamed_tweets = []
                store_tweets(stored_tweets)

            # Print out total tweets everytime we get a tweet
            print (f"\r>> {total_tweets} tweets... ", end='', flush=True)

# Authenticate into the twitter streaming api
streaming_client = TweetStorer(bearer_token, wait_on_rate_limit=True)

# Modify the stream rules on the streaming client
def rule_in_list(rule: tweepy.StreamRule, list: List[tweepy.StreamRule]) -> bool:
    for curr_rule in list:
        if rule.value == curr_rule.value:
            return True 
    
    return False

file_rules = [ tweepy.StreamRule(stream_rule["rule"], stream_rule["tag"]) for stream_rule in stream_rules ]
current_rules: List[tweepy.StreamRule] = streaming_client.get_rules().data
if current_rules is None:
    current_rules = []

add_rules = [rule for rule in file_rules if not rule_in_list(rule, current_rules) ]
remove_rules = [rule for rule in current_rules if not rule_in_list(rule, file_rules)]

if len(remove_rules) > 0:
    print(f"Removing {len(remove_rules)} rules from the streaming endpoint!")
    rules_response = streaming_client.delete_rules([ rule.id for rule in remove_rules ])

    if rules_response.errors is not None and len(rules_response.errors) != 0:
        print("Error while removing stream rules!")
        print(rules_response.errors)
        exit(1)

if len(add_rules) > 0:
    print(f"Adding {len(add_rules)} rules to the streaming endpoint!")
    rules_response = streaming_client.add_rules(add_rules)

    if rules_response.errors is not None and len(rules_response.errors) != 0:
        print("Error while adding stream rules!")
        print(rules_response.errors)
        exit(1)

print(f"Using {(len(add_rules) + len(current_rules)) - len(remove_rules)} rules on the streaming endpoint!")

# Run filter stream
tweet_fields = ["id", "text", "author_id", "created_at", "geo", "source", "public_metrics" ]
start_stream_time = time.time()
print("Starting twitter stream...")
streaming_client.filter(threaded=True, tweet_fields=tweet_fields)

# Wait for user to type in quit or Ctrl+C
try:
    while input("Type 'quit' to stop streaming...\n") != "quit":
        continue
except EOFError as eof:
    print("Caught force-quit...")
except KeyboardInterrupt as interrupt:
    print("Caught force-quit...")

print(f"Stopping stream...")

if streaming_client.running:
    streaming_client.disconnect()

if len(streamed_tweets) > 0:
    print(f"Storing left-over tweets...")
    store_tweets(streamed_tweets)

# Print out tweet metrics
def tweet_stream_metrics():
    global start_stream_time
    global total_tweets
    global file_num

    end = time.time()
    seconds_elapsed = end - start_stream_time
    import datetime
    time_str = str(datetime.timedelta(seconds=seconds_elapsed))
    print(f"Collected {total_tweets} tweets in {time_str}!")
    print(f"Tweets Collected Per Minute: {(total_tweets * 60.0) / seconds_elapsed} tweets!")
    print(f"Created {file_num} files to store the tweets!")
    if file_num > 0:
        print(f"A file to store the tweets was created about every {seconds_elapsed / file_num} seconds!")
        # File sizes are in bytes
        file_sizes = [ file_size(f"{FILE_PREFIX}{f_num}.json") for f_num in range(file_num) ]
        # Get total data size in MB (1 mil bytes in 1 MB)
        total_data_size = sum(file_sizes) / 1000000.0
        print("Total Data Size Collected: " + "{:.2f}".format(total_data_size) + " MB")
        print("Average Data Per File: " + "{:.2f}".format(total_data_size / len(file_sizes)) + " MB")
        

tweet_stream_metrics()

