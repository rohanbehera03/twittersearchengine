# Instructions for installation
1. Requirements: Java SE Development Kit 8u333, Apache Maven, Visual Studio or other
IDE
2. Download the zip file, unzip and open the folder in your IDE
3. In your terminal window press cd, enter and then vim .bash_profile. Make sure the paths
to maven_home and java_home are set correctly. Then close out of vim and run source
.bash_profile
4. Run the command mvn
5. cd into the folder where the source code is and run mvn clean install
6. Then run the command mvn spring:boot run
7. Open up a new tab in your browser and type in localhost:9090
8. Search for tweets!
