package de.ed0cinu.nekoslifescraper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class NekosLifeScraper {

    private static boolean debug = true;
    private static int nextScrapmaxScrapeDelay = 1000, maxScrapeDuplicateCount = 10, maxScrapesPerEndpoint = 100;

    private final static String nekosLifeDomain = "nekos.life", nekosLifeApiDefaultUrl = "https://" + nekosLifeDomain + "/api/", nekosLifeCdnDefaultUrl = "https://cdn." + nekosLifeDomain + "/", responseDataSplitterStart = "\":\"", responseDataSplitterEnd = "\"}";

    private final static List<String> endPoints = Arrays.asList(
            "hug",
            "kiss",
            "lewd/neko",
            "neko",
            "pat",
            "v2/img/smug",
            "v2/img/woof",
            "v2/img/gasm",
            "v2/img/cuddle",
            "v2/img/avatar",
            "v2/img/slap",
            "v2/img/pat",
            "v2/img/gecg",
            "v2/img/feed",
            "v2/img/fox_girl",
            "v2/img/neko",
            "v2/img/hug",
            "v2/img/meow",
            "v2/img/kiss",
            "v2/img/wallpaper",
            "v2/img/tickle",
            "v2/img/spank",
            "v2/img/waifu",
            "v2/img/lewd",
            "v2/img/ngif"
    );

    private static boolean isDoneWithCurrentScrape = false, isScraping = false;

    private static final List<String> currentEndpointScrapedFiles = new ArrayList<>();
    private static int currentEndpointId = 0, duplicateScrapeCount = 0, scrapeCount = 0, scrapingThreads = 0;

    private static void addRequestProperties(final HttpURLConnection downloadConnection) throws ProtocolException {
        downloadConnection.setRequestMethod("GET");
        downloadConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
        downloadConnection.setRequestProperty("Host", nekosLifeDomain);
        downloadConnection.setRequestProperty("Sec-Fetch-Dest", "document");
        downloadConnection.setRequestProperty("Sec-Fetch-Mode", "navigate");
        downloadConnection.setRequestProperty("Sec-Fetch-Site", "cross-site");
        downloadConnection.setRequestProperty("TE", "trailers");
        downloadConnection.setRequestProperty("Upgrade-Insecure-Requests", "1");
        downloadConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/112.0");
    }

    private static void scrapeEndpoint(final String endpoint) {
        if (isScraping || isDoneWithCurrentScrape || scrapingThreads >= 1) return;
        isScraping = true;
        final String currentEndpoint = nekosLifeApiDefaultUrl + endpoint;
        new Thread(() -> {
            System.out.println("Current Scrape: " + (currentEndpointScrapedFiles.size() + 1) + "/" + maxScrapesPerEndpoint + " (" + scrapeCount + "/" + (maxScrapesPerEndpoint * endPoints.size()) + ")");
            scrapingThreads++;
            boolean failed = false;
            String failReason = "";
            try {
                final URL url = new URL(currentEndpoint);
                final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                addRequestProperties(connection);
                final int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    final BufferedReader getStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    final StringBuilder response = new StringBuilder();
                    while ((inputLine = getStream.readLine()) != null) response.append(inputLine);
                    getStream.close();
                    final String responseData = response.toString();
                    if (responseData.contains(responseDataSplitterStart) && responseData.endsWith(responseDataSplitterEnd)) {
                        final String responseValue = responseData.split(responseDataSplitterStart)[1].split(responseDataSplitterEnd)[0];
                        final HttpURLConnection downloadConnection = (HttpURLConnection) new URL(responseValue).openConnection();
                        addRequestProperties(downloadConnection);
                        final int downloadResponseCode = downloadConnection.getResponseCode();
                        if (downloadResponseCode == HttpURLConnection.HTTP_OK) {
                            final String shortPath = responseValue.replaceFirst(nekosLifeCdnDefaultUrl, "").replace("\\", "/");
                            final String[] folderAndFile = shortPath.split("/");
                            final File downloadFolder = new File(folderAndFile[0]);
                            if (!downloadFolder.exists()) downloadFolder.mkdirs();
                            final File downloadFile = new File(downloadFolder, folderAndFile[1]);
                            final String name = downloadFile.getName();
                            if (currentEndpointScrapedFiles.contains(name)) {
                                duplicateScrapeCount++;
                                failed = true;
                                failReason = "Duplicate: " + name;
                            }
                            else {
                                currentEndpointScrapedFiles.add(name);
                                BufferedInputStream downloadStream = null;
                                FileOutputStream fileOutputStream = null;
                                if (!downloadFile.exists()) {
                                    try {
                                        downloadStream = new BufferedInputStream(downloadConnection.getInputStream());
                                        fileOutputStream = new FileOutputStream(downloadFile);
                                        final int dataBufferSize = 1024;
                                        final byte[] dataBuffer = new byte[dataBufferSize];
                                        int bytesRead;
                                        while ((bytesRead = downloadStream.read(dataBuffer, 0, dataBufferSize)) != -1) fileOutputStream.write(dataBuffer, 0, bytesRead);
                                        downloadStream.close();
                                        fileOutputStream.close();
                                        System.out.println("Scraped '" + responseValue + "' into '" + downloadFile.getAbsolutePath() + "'");
                                    }
                                    catch (final IOException ioe) {
                                        if (debug) ioe.printStackTrace();
                                        failed = true;
                                        final String splitter = "#-#-#-#";
                                        failReason = "IO Exception: " + ioe.toString().replaceFirst(": ", splitter).split(splitter)[1];
                                    }
                                }
                                else {
                                    failed = true;
                                    failReason = "File Already exists: " + name;
                                }
                                if (downloadStream != null) downloadStream.close();
                                if (fileOutputStream != null) fileOutputStream.close();
                            }
                        }
                        else {
                            failed = true;
                            failReason = "Invalid Download Response Code: " + responseCode;
                        }
                    }
                    else {
                        failed = true;
                        failReason = "Invalid Endpoint Response Data: " + responseData;
                    }
                }
                else {
                    failed = true;
                    failReason = "Invalid Endpoint Response Code: " + responseCode;
                }
            }
            catch (final Throwable throwable) {
                if (debug) throwable.printStackTrace();
                failed = true;
                failReason = "Error: " + throwable;
            }
            if (failReason.startsWith("Duplicate: ")) {
                if (!debug) failed = false;
            }
            if (failed) {
                System.err.println("Failed to scrape '" + currentEndpoint + "' caused by " + failReason);
            }
            isDoneWithCurrentScrape = true;
            scrapingThreads--;
            if (scrapingThreads < 0) scrapingThreads = 0;
        }, currentEndpoint + "-Scrape-Thread").start();
    }

    private static String getEndpointScrapeFilesText() {
        return "Scraped " + currentEndpointScrapedFiles.size() + " Files from the Endpoint '" + endPoints.get(currentEndpointId) + "'";
    }

    public static void main(final String... args) {
        System.out.println("/-----------------------------------------------------------------------------------------\\");
        System.out.println("|    _   _      _     (>-<)     _     _  __       _____           *rawr*                  |");
        System.out.println("|   | \\ | |    | |             | |   (_)/ _|     /  ___|                                  |");
        System.out.println("|   |  \\| | ___| | _____  ___  | |    _| |_ ___  \\ `--.  ___ _ __ __ _ _ __   ___ _ __    |");
        System.out.println("|   | . ` |/ _ \\ |/ / _ \\/ __| | |   | |  _/ _ \\  `--. \\/ __| '__/ _` | '_ \\ / _ \\ '__|   |");
        System.out.println("|   | |\\  |  __/   < (_) \\__ \\ | |___| | ||  __/ /\\__/ / (__| | | (_| | |_) |  __/ |      |");
        System.out.println("|   \\_| \\_/\\___|_|\\_\\___/|___/ \\_____/_|_| \\___| \\____/ \\___|_|  \\__,_| .__/ \\___|_|      |");
        System.out.println("|                                                                      | |     OwO        |");
        System.out.println("|    *boop*                    > Made by ed0cinU <                    |_|                 |");
        System.out.println("|                 UwU                                      ~nya                           |");
        System.out.println("|                                _._     _,-'\"\"`-._                              <><      |");
        System.out.println("|                              (,-.`._,'(       |\\`-/|                         <>< <><    |");
        System.out.println("|        *nya* *nya*               `-.-' \\  )-`( , o o)                      <>< <>< <><  |");
        System.out.println("|                                         `-    \\`_`\"'-   <><  <><  <><     \\´_´_'_`_`_/  |");
        System.out.println("\\-----------------------------------------------------------------------------------------/");
        System.out.println();
        if (args.length < 4) {
            System.out.println("Usage: java -jar nekoslifescraper.jar <maxScrapesPerEndpoint> <maxScrapeDuplicateCount> <maxScrapeDelay> <debug>");
            System.out.println("Example: java -jar nekoslifescraper.jar 100 10 1000 true");
            System.out.println();
            System.out.println("Max Scrapes Per Endpoint -> The max Number of Files that getting scraped from the Endpoint.");
            System.out.println("Max Scrape Duplicate Count -> The max Number of Duplicates before the Endpoint gets skipped.");
            System.out.println("Max Scrape Delay -> The Delay that the Scraper has to wait before starting the next Scraper.");
            System.out.println("Debug -> If enabled it prints more Information and Error Output into the Console.");
            System.out.println();
            return;
        }
        try {
            maxScrapesPerEndpoint = Integer.parseInt(args[0]);
        }
        catch (final NumberFormatException ignored) {
            System.err.println(args[0] + " is an invalid Integer! Do \"java -jar nekoslifescraper.jar\" for help.");
            return;
        }
        try {
            maxScrapeDuplicateCount = Integer.parseInt(args[1]);
        }
        catch (final NumberFormatException ignored) {
            System.err.println(args[1] + " is an invalid Integer! Do \"java -jar nekoslifescraper.jar\" for help.");
            return;
        }
        try {
            nextScrapmaxScrapeDelay = Integer.parseInt(args[2]);
        }
        catch (final NumberFormatException ignored) {
            System.err.println(args[2] + " is an invalid Integer! Do \"java -jar nekoslifescraper.jar\" for help.");
            return;
        }
        try {
            debug = Boolean.parseBoolean(args[3]);
        }
        catch (final NumberFormatException ignored) {
            System.err.println(args[3] + " is an invalid Boolean! Do \"java -jar nekoslifescraper.jar\" for help.");
            return;
        }
        new Thread(() -> {
            System.out.println("Starting with the Scrape of Nekos Life...");
            boolean firstScrape = true;
            int printStartingCount = 0;
            while (true) {
                if (!isScraping) {
                    String endpointFileCountText = "";
                    final int lastEndpointId = currentEndpointId;
                    if (currentEndpointScrapedFiles.size() >= maxScrapesPerEndpoint) {
                        endpointFileCountText = getEndpointScrapeFilesText();
                        currentEndpointId++;
                        currentEndpointScrapedFiles.clear();
                    }
                    else if (duplicateScrapeCount >= maxScrapeDuplicateCount) {
                        duplicateScrapeCount = 0;
                        endpointFileCountText = getEndpointScrapeFilesText();
                        currentEndpointScrapedFiles.clear();
                        currentEndpointId++;
                    }
                    if (!endpointFileCountText.isEmpty()) System.out.println(endpointFileCountText);
                    final boolean doneWithLastEndpoint = lastEndpointId != currentEndpointId;
                    if (doneWithLastEndpoint) {
                        System.out.println("Done with scraping the Endpoint '" + endPoints.get(lastEndpointId) + "'");
                    }
                    if (currentEndpointId >= endPoints.size()) {
                        System.out.println("Done with scraping all Endpoints. Scraped-Files: " + scrapeCount);
                        break;
                    }
                    if (firstScrape || doneWithLastEndpoint) {
                        firstScrape = false;
                        System.out.println("Now scraping the Endpoint '" + endPoints.get(currentEndpointId) + "'");
                    }
                    scrapeEndpoint(endPoints.get(currentEndpointId));
                }
                else {
                    if (isDoneWithCurrentScrape) {
                        final long delay = ThreadLocalRandom.current().nextInt(nextScrapmaxScrapeDelay, nextScrapmaxScrapeDelay + 500);
                        System.out.println("Waiting " + delay + "ms...");
                        try {
                            Thread.sleep(delay);
                        }
                        catch (final Throwable ignored) {}
                        System.out.println("Running next Scrape.");
                        isScraping = false;
                        isDoneWithCurrentScrape = false;
                        scrapeCount++;
                        printStartingCount = 0;
                    }
                    else {
                        printStartingCount++;
                        switch (printStartingCount) {
                            case 100000000: {
                                System.out.println("Starting the Scraper");
                                break;
                            }
                            case 300000000: {
                                System.out.println("Starting the Scraper.");
                                break;
                            }
                            case 400000000: {
                                System.out.println("Starting the Scraper..");
                                printStartingCount = 0;
                                break;
                            }
                            case 500000000: {
                                System.out.println("Starting the Scraper...");
                                printStartingCount = 0;
                                break;
                            }
                            default: break;
                        }
                    }
                }
            }
            System.out.println("Done with the Scrape of Nekos Life.");
        }, "Main-Scrape-Thread").start();
    }


}