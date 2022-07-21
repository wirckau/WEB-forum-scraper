# WEB-forum-scraper
Fetches a web forum to retrieve recently added thread / post.
Starts from the forum page, captures a list of new / updated forum threads in a queue and navigate the newest pages before moves on to the next thread in the list. Stores the extracted data from the post locally.

## Executing the JAR:
In order to encode the Freemarker template in UTF-8 when deployed in the JAR
in JAVA options/parameters the Environment variable has to be picked up:
```bash
JAVA_TOOL_OPTIONS: -D"file.encoding=UTF-8"
```
