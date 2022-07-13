import tweepy
from dotenv import dotenv_values
import json

# Constants
MAX_TWEETS=10
QUERY="(#marvel OR #ironman OR #doctorstrange OR #spiderman OR #antman OR #scarletwitch OR #wandavision) -is:retweet lang:en"

# Validate constants
if MAX_TWEETS < 10 or MAX_TWEETS > 100:
    print("ERROR: Max tweets must be between 10 and 100")
    exit(1)

# Load .env file
config = dotenv_values(".env")
bearer_token = config["BEARER_TOKEN"]

# Fields on the tweet to get
tweet_fields = ["id", "text", "author_id", "created_at", "geo", "source", "public_metrics" ]

# Authenticate into the twitter api
client = tweepy.Client(bearer_token=bearer_token)

# Search recent tweets based on specified tweet fields and the most relevant.                        
response = client.search_recent_tweets(QUERY, tweet_fields=tweet_fields, max_results=MAX_TWEETS, sort_order="relevancy")

tweets = response.data

# Handle Errors
if tweets is None:
    print("No tweets were found!")
    if response.errors is not None:
        print("Errors Occured:")
        print(response.errors)

    exit(1)

"""
Tweet ID
User ID
Time/date tweeted
Device tweeted on
Geolocation
Text of tweet
# of Likes
# of retweets
"""

# Convert a tweet to a python dictionary based on the relevant fields
def tweet_to_dict(tweet) -> dict:
    # print(tweet.keys())

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

filtered_tweets = filter(filter_tweet, tweets)

# Write the filtered tweets to JSON
with open('tweets.json', 'w') as f:
    f.write('[')
    tweets_json = [ json.dumps(tweet_to_dict(tweet), indent=4, default=str) for tweet in filtered_tweets ]
    f.write(",\n".join(tweets_json))
    f.write("]")


