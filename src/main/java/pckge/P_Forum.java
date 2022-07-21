package pckge;

import com.codeborne.selenide.*;

import pckge.commons.Locator;
import pckge.commons.Settings;
import pckge.commons.Browser;
import pckge.commons.GetDateInput;
import pckge.commons.UpdateThreadList;

import pckge.model.Article;
import pckge.model.DateParser;
import pckge.model.Post;

import org.openqa.selenium.By;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import static com.codeborne.selenide.Selenide.*;

public class P_Forum {
    private static final SelenideElement GDPR_CONSENT = $(By.xpath("//button[@class=' css-4jk']"));
    private static final AbstractList<SelenideElement> COMMENT_ITEM = $$(By.xpath("(//tr[@id='td2'])"));
    private static final AbstractList<SelenideElement> PREV_PG_EXIST = $$(By.xpath("(//li[@class='nextpage'])[2]"));
    private static final SelenideElement PREV_PG_BTN = $(By.xpath("((//li[@class='nextpage'])/a)[2]"));
    private static final SelenideElement NXT_PG_BTN = $(By.xpath("((//li[@class='nextpage'])/a)[3]"));
    private static final SelenideElement VIDEO_EMBED = $(By.xpath("//div[@class='play']"));
    private static final SelenideElement USER_NAME = $(By.name("user"));
    private static final SelenideElement USER_PSW = $(By.name("pass"));
    private static final SelenideElement LOGIN_BTN = $(By.name("login"));
    private static final SelenideElement MODAL_RTR = $(By.cssSelector("center:nth-child(2) > a:nth-child(1)"));

    static String author = "//tr[@id='td1'][%d]/td/a[@class]";
    static String postTime = "(//tr/td/font[@id='post_time'])[%d]";
    static String comment = "(//tr[@id='td2'])[%d]";

    private static Map<String, String> threadListHash;

    private DateTimeFormatter dtf;
    private Writer writer = null;
    private Writer outputWriter;
    private Configuration config;
    private Template template;
    private Map<String, Object> map;
    private List<Article> listedThreads;
    private List<Post> posts;
    private Date crawlFromDate;

    P_Forum() throws IOException {
        crawlFromDate = GetDateInput.DateInput("p");
        System.out.println("Crawling from loaded date " + crawlFromDate);

        //com.codeborne.selenide.Configuration.browser = "firefox";

        Browser.startPage(Settings.baseUrlP() + "/", false);
        if (GDPR_CONSENT.exists()) {
            GDPR_CONSENT.shouldBe(Condition.visible).click();
        } //bypass gdpr consent modal

        dtf = DateTimeFormatter.ofPattern("dd. MMM yyyy, HH:mm", Locale.ENGLISH);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH-mm");
        outputWriter = new FileWriter(
                new File(Settings.downloadPath() + "Data " + sdf.format(new Date()) + ".html"));

        config = new freemarker.template.Configuration(Configuration.VERSION_2_3_23);

        config.setDefaultEncoding("UTF-8");
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        config.setClassForTemplateLoading(Launcher.class, "/template");
        template = config.getTemplate("html.ftl");

        map = new HashMap<>();
        listedThreads = new ArrayList<>();
        posts = new ArrayList<>();
    }

    private void videoExpander() throws InterruptedException {
        if ($(VIDEO_EMBED).exists()) {
            do {
                $(VIDEO_EMBED).click();
                Thread.sleep(3000);
            } while ($(VIDEO_EMBED).exists());
        }
    }

    public static void main(String[] args) throws Exception {

        threadListHash = new UpdateThreadList().methodRead(Settings.folderPath() + "./writtenListP.txt");
        new P_Forum().startFetch();

    }

    private void startFetch() throws IOException, TemplateException {
        try {
            map.put("pageTitle", "P_forum");
            System.out.println("Loading the thread list..");

            threadListHash.forEach((key, value) -> listedThreads.add(
                    new Article(key
                            , value)));
            System.out.println("Threads scheduled: " + listedThreads.size());

            //output all Threads
            List<Article> threads = new ArrayList<>(listedThreads);
            map.put("ulThreads", threads);
            template.process(map, outputWriter);
            map.clear();

            Runtime.getRuntime().exec("wscript " + Settings.awakerPath()); //stay awake
            for (int threadIdx = 0; threadIdx < listedThreads.size(); threadIdx++) {
                System.out.println("Thread " + (threadIdx + 1) + "/" + listedThreads.size());

                //re-instantiate every Thread in order to process to html one by one
                threads = new ArrayList<>();
                threads.add(listedThreads.get(threadIdx));
                map.put("threads", threads);

                open(listedThreads.get(threadIdx).getUrl());

                //requires login:
                if ($(By.xpath("//h1[contains(., 'Member')]")).exists()) {
                    System.out.println("login thread found");
                    String restrictedThread = WebDriverRunner.getWebDriver().getCurrentUrl();
                    $(USER_NAME).click();
                    $(USER_NAME).sendKeys(Settings.usr());
                    $(USER_PSW).click();
                    $(USER_PSW).sendKeys(Settings.psw());
                    $(LOGIN_BTN).click();
                    open(restrictedThread);
                    $(MODAL_RTR).click();
                }

                String currentThreadUrl = WebDriverRunner.getWebDriver().getCurrentUrl();

                // IF exists prevPage and inserted datetime is before earliestPost datetime
                // THEN do iteration on prevPages
                if (!$(Locator.xPath(postTime, 1)).exists())
                    if ($(PREV_PG_BTN).exists())    // case post deleted
                        $(PREV_PG_BTN).click();     // on last pg
                    else continue;
                DateParser earliestPost = new
                        DateParser($(Locator.xPath(postTime, 1)).getText());
                if (PREV_PG_EXIST.size() == 1 && crawlFromDate.compareTo(earliestPost.getDate()) <= 0) {
                    //paging back
                    do {
                        $(PREV_PG_BTN)
                                .shouldBe(Condition.visible, Duration.ofMinutes(1))
                                .click();
                        earliestPost = new
                                DateParser($(Locator.xPath(postTime, 1)).getText());
                    }
                    while (PREV_PG_EXIST.size() == 1
                            && crawlFromDate.compareTo(earliestPost.getDate()) <= 0
                            && (!(WebDriverRunner.getWebDriver().getCurrentUrl().equals(currentThreadUrl))));
                }

                //writer loop
                int prevPgClickerCnt = 1;
                String currentPositionUrl;
                posts = new ArrayList<>();
                do {
                    for (int commentNr = 1; commentNr <= COMMENT_ITEM.size(); commentNr++) {
                        if (COMMENT_ITEM.size() < 1)
                            break;
                        //writer
                        DateParser post = new
                                DateParser($(Locator.xPath(postTime, commentNr)).getText());
                        if (crawlFromDate.compareTo(post.getDate()) <= 0) {
                            videoExpander();
                            prevPgClickerCnt = 0; //prevPgClicker cnt resets

                            posts.add(new Post($(Locator.xPath(author, commentNr)).getText()
                                    , $(Locator.xPath(postTime, commentNr)).getText()
                                    , $(Locator.xPath(comment, commentNr)).getAttribute("innerHTML")
                                    .replace("border: none; visibility: visible; width: 0px; height: 0px"
                                            , "margin-top: 1px") // Rpl for FB content handling
                                    .replace("border: none; visibility: hidden"
                                            , "margin-top: 1px")));
                            System.out.println("Post " + commentNr + " written");
                        }
                    }
                    map.put("posts", posts);

                    currentPositionUrl = WebDriverRunner.getWebDriver().getCurrentUrl();
                    if (!(WebDriverRunner.getWebDriver().getCurrentUrl().equals(currentThreadUrl)))
                        $(NXT_PG_BTN).click();

                    prevPgClickerCnt += 1;
                } while (!(currentPositionUrl.equals(currentThreadUrl))
                        && prevPgClickerCnt <= 3);

                template.process(map, outputWriter);
                map.clear();

                //Skipper Btn block:
                try {
                    if (listedThreads.size() - 1 > listedThreads.indexOf(listedThreads.get(threadIdx))) {
                        //System.out.println("Next skipper added");
                        threads = new ArrayList<>();
                        threads.add(listedThreads.get(threadIdx + 2));
                        map.put("nxtThreads", threads);
                        template.process(map, outputWriter);
                    }
                } catch (Exception ex) {
                    System.out.println("Next Skipper ex..");
                }

                map.clear();
            } // End of Thread processing

            outputWriter.flush();
            writer = new OutputStreamWriter(
                    new FileOutputStream(Settings.folderPath() + "dateFactory.txt"), StandardCharsets.UTF_8);
            writer.write("" + dtf.format(LocalDateTime.now()) + "");
            System.out.println("Fetch end; File datetime updated to " + dtf.format(LocalDateTime.now()));
            new File(Settings.folderPath() + "writtenListP.txt").delete();
            System.out.println("deleted file! ");
        } catch (Throwable e) {
            System.out.println("Ex thrown: " + e);
            e.printStackTrace();
            map.clear();
            map.put("error", String.valueOf(e));
            outputWriter.flush();
            template.process(map, outputWriter);
        } finally {
            try {
                if (writer != null)
                    writer.close();
                if (outputWriter != null)
                    outputWriter.close();
                Selenide.closeWindow();
                Selenide.closeWebDriver(); // Should close the whole browser, Selenide 5.2 bug
                Runtime.getRuntime().exec("taskkill /F /IM chromedriver.exe /T"); //tskmng
            } catch (Exception ex) {
                System.out.println("Cant close writer/driver");
            }
        }
    }
}