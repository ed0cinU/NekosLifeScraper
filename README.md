# NekosLifeScraper
A Scraper for the nekos.life API written in Java. It scrapes almost all Files in a simple naive way.


# How to use:

```

/-----------------------------------------------------------------------------------------\
|    _   _      _     (>-<)     _     _  __       _____           *rawr*                  |
|   | \ | |    | |             | |   (_)/ _|     /  ___|                                  |
|   |  \| | ___| | _____  ___  | |    _| |_ ___  \ `--.  ___ _ __ __ _ _ __   ___ _ __    |
|   | . ` |/ _ \ |/ / _ \/ __| | |   | |  _/ _ \  `--. \/ __| '__/ _` | '_ \ / _ \ '__|   |
|   | |\  |  __/   < (_) \__ \ | |___| | ||  __/ /\__/ / (__| | | (_| | |_) |  __/ |      |
|   \_| \_/\___|_|\_\___/|___/ \_____/_|_| \___| \____/ \___|_|  \__,_| .__/ \___|_|      |
|                                                                      | |     OwO        |
|    *boop*                    > Made by ed0cinU <                    |_|                 |
|                 UwU                                      ~nya                           |
|                                _._     _,-'""`-._                              <><      |
|                              (,-.`._,'(       |\`-/|                         <>< <><    |
|        *nya* *nya*               `-.-' \  )-`( , o o)                      <>< <>< <><  |
|                                         `-    \`_`"'-   <><  <><  <><     \´_´_'_`_`_/  |
\-----------------------------------------------------------------------------------------/

Usage: java -jar nekoslifescraper.jar <maxScrapesPerEndpoint> <maxScrapeDuplicateCount> <maxScrapeDelay> <debug>
Example: java -jar nekoslifescraper.jar 100 10 1000 true

Max Scrapes Per Endpoint -> The max Number of Files that getting scraped from the Endpoint.
Max Scrape Duplicate Count -> The max Number of Duplicates before the Endpoint gets skipped.
Max Scrape Delay -> The Delay that the Scraper has to wait before starting the next Scraper.
Debug -> If enabled it prints more Information and Error Output into the Console.

```
