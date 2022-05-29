package pckge;

import com.codeborne.selenide.*;
import freemarker.template.Template;
import helpers.Commons;
import org.openqa.selenium.By;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import pckge.model.Article;
import pckge.model.DateParser;
import pckge.model.Post;

import static com.codeborne.selenide.Selenide.*;

public class P_Forum {
    //elements:
    private static final SelenideElement gdprConsent = $(By.xpath("//button[@class=' css-4jk']"));
    private static final AbstractList<SelenideElement> activeThreadList = $$(By
            .xpath("(//img[@title]//following::td[@width='79%'])"));
    private static final AbstractList<SelenideElement> commentItem = $$(By.xpath("(//tr[@id='td2'])"));
    private static final AbstractList<SelenideElement> prevPgExist = $$(By.xpath("(//li[@class='nextpage'])[2]"));
    private static final SelenideElement prevPgBtn = $(By.xpath("((//li[@class='nextpage'])/a)[2]"));
    private static final SelenideElement nxtPgBtn = $(By.xpath("((//li[@class='nextpage'])/a)[3]"));
    private static final SelenideElement videoEmbed = $(By.xpath("//div[@class='play']"));
    private static final SelenideElement userName = $(By.name("user"));
    private static final SelenideElement userPsw = $(By.name("pass"));
    private static final SelenideElement loginBtn = $(By.name("login"));
    private static final SelenideElement modalRtr = $(By.cssSelector("center:nth-child(2) > a:nth-child(1)"));

    static String author = "//tr[@id='td1'][%d]/td/a[@class]";
    static String postTime = "(//tr/td/font[@id='post_time'])[%d]";
    static String comment = "(//tr[@id='td2'])[%d]";
    static String threadLastActive = "(//img[@title]//following::td[contains(@width,'12')])[%d]";
    static String activity = "(//a[@class='%s'])[%d]";

    static Commons c = new Commons();

    Date dateTimeInput;
    public P_Forum() throws ParseException, IOException {
        SimpleDateFormat dateFormatter =
                new SimpleDateFormat("dd. MMM yyyy, HH:mm", Locale.ENGLISH);
        String contentDateFrom = new String(Files.readAllBytes(Paths.get(c.folderPath() + "dateFactory.txt")));
        dateTimeInput = dateFormatter.parse(contentDateFrom);
        System.out.println("Crawling from loaded date "+ dateTimeInput);
    }

    //wrappers:
    public static By xPathLocatorPattern(String locator, int no) {
        return By.xpath(String.format(locator, no));
    }
    public static By xPathLocatorPattern(String locator, String param1, int no) {
        return By.xpath(String.format(locator, param1, no));
    }

    static void openPage() {

        com.codeborne.selenide.Configuration.startMaximized = true;
        com.codeborne.selenide.Configuration.timeout = 20000;
        com.codeborne.selenide.Configuration.pageLoadStrategy = "eager";
        open(c.baseUrlP()+"/");
        if (gdprConsent.exists()) {
            gdprConsent.shouldBe(Condition.visible).click();
        } //bypass gdpr modal
    }
    static String openForum(int forumNr) {
        String url;
        switch (forumNr) {
            case 1:
                url = c.baseUrlP()+"/latest.php?";
                break;
            case 2:
                url = c.baseUrlM();
                break;
            default:
                throw new IllegalArgumentException("Invalid forum feed: " + forumNr);
        }
        return url;
    }
    static String indexForum(int forumNr) {
        String threadIdx;
        switch (forumNr) {
            case 1:
                threadIdx = "3";
                break;
            case 2:
                threadIdx = "c2";
                break;
            default:
                throw new IllegalArgumentException("Invalid forum index: " + forumNr);
        }
        return threadIdx;
    }

    static void videoExpander() throws InterruptedException {
        if ($(videoEmbed).exists()) {
            do {
                $(videoEmbed).click();
                Thread.sleep(3000);
            } while ($(videoEmbed).exists());
        }
    }

    public static void main(String[] args)throws Exception {
        P_Forum page = new P_Forum();

        openPage();

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH-mm");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd. MMM yyyy, HH:mm", Locale.ENGLISH);

        Writer writer = null;
        Writer outputWriter = new FileWriter (
                new File(c.folderPath() + "Data " + sdf.format(new Date()) + ".html"));

        Configuration config = new Configuration(Configuration.VERSION_2_3_23);
        config.setDirectoryForTemplateLoading(new File("./"));
        config.setDefaultEncoding("UTF-8");
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        Template template = config.getTemplate("template.ftl");

        Map<String, Object> map = new HashMap<>();
        map.put("pageTitle", "P_forum");
        try {
            Runtime.getRuntime().exec( "wscript "+c.awakerPath() ); //stay awake
            for(int forumIdx=1; forumIdx<=2; forumIdx++) {   ///two forums
                open(openForum(forumIdx));

                List<Article> listedThreads = new ArrayList<>();

                System.out.println("Starts fetching the thread data..");
                System.out.println("Threads detected: "+activeThreadList.size());

                for (int i = 1; i <= activeThreadList.size(); i++) {
                    DateParser lastActivity;
                    lastActivity = new
                            DateParser($(xPathLocatorPattern(threadLastActive, i)).getText());
                    if(page.dateTimeInput.compareTo(lastActivity.getConvActivityDate()) <= 0) {
                        listedThreads.add(
                                new Article(($(xPathLocatorPattern(activity,indexForum(forumIdx),i))
                                        .getAttribute("title"))
                                        ,($(xPathLocatorPattern(activity,indexForum(forumIdx),i))
                                        .getAttribute("href"))));
                    }
                }
                System.out.println("Threads scheduled: " + listedThreads.size());

                List<Article> threads = new ArrayList<>(listedThreads);
                map.put("ulThreads", threads);
                template.process(map, outputWriter);
                map.clear();

                System.out.println("Starts Thread looping from forum "+forumIdx);
                for (int threadIdx = 0; threadIdx<listedThreads.size(); threadIdx++) {
                    System.out.println("Thread " + (threadIdx+1) + "/" + listedThreads.size());

                    //re-instantiate every Thread in order to process to html one by one
                    threads = new ArrayList<>();
                    threads.add(listedThreads.get(threadIdx));
                    map.put("threads", threads);

                    open(listedThreads.get(threadIdx).getUrl());

                    //requires login:
                    if ($(By.xpath("//h1[contains(., 'Member')]")).exists()) {
                        System.out.println("login thread found");
                        String restrictedThread = WebDriverRunner.getWebDriver().getCurrentUrl();
                        $(userName).click();
                        $(userName).sendKeys(c.usr());
                        $(userPsw).click();
                        $(userPsw).sendKeys(c.psw());
                        $(loginBtn).click();
                        open(restrictedThread);
                        $(modalRtr).click();
                    }

                    String currentThreadUrl = WebDriverRunner.getWebDriver().getCurrentUrl();

                    // IF exists prevPage and inserted datetime is before earliestPost datetime
                    // THEN do iteration on prevPages
                    DateParser earliestPost = new
                            DateParser($(xPathLocatorPattern(postTime, 1)).getText());
                    if (prevPgExist.size() == 1 && page.dateTimeInput.compareTo(earliestPost.getDate()) <= 0) {
                        //paging back
                        do {
                            $(prevPgBtn)
                                    .shouldBe(Condition.visible, Duration.ofMinutes(1))
                                    .click();
                            earliestPost = new
                                    DateParser($(xPathLocatorPattern(postTime, 1)).getText());
                        }
                        while (prevPgExist.size() == 1
                                && page.dateTimeInput.compareTo(earliestPost.getDate()) <= 0
                                && (!(WebDriverRunner.getWebDriver().getCurrentUrl().equals(currentThreadUrl))));
                    }

                    //writer loop
                    int prevPgClickerCnt = 1;
                    String currentPositionUrl;
                    List<Post> posts = new ArrayList<>();
                    do {
                        for (int commentNr = 1; commentNr <= commentItem.size(); commentNr++) {
                            //writer
                            DateParser post = new
                                    DateParser($(xPathLocatorPattern(postTime, commentNr)).getText());
                            if (page.dateTimeInput.compareTo(post.getDate()) <= 0) {
                                videoExpander();
                                prevPgClickerCnt = 0; //prevPgClicker cnt resets

                                posts.add(new Post($(xPathLocatorPattern(author, commentNr)).getText()
                                        , $(xPathLocatorPattern(postTime, commentNr)).getText()
                                        , $(xPathLocatorPattern(comment, commentNr)).getAttribute("innerHTML")
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
                            $(nxtPgBtn).click();

                        prevPgClickerCnt += 1;
                    } while (!(currentPositionUrl.equals(currentThreadUrl))
                            && prevPgClickerCnt <= 3);

                    //due to paging fail [case eager mode load DOM] add error and add Thread at the end
                    if (prevPgClickerCnt > 3) {
                        map.put("error", "Reached too many Pg without writing a comment");
                        System.out.println("Stopper reached! error written @ " + prevPgClickerCnt);
                        open(openForum(forumIdx)); //navigate to ThreadActivity list to get the title>
                        listedThreads.add(
                                new Article(($(xPathLocatorPattern(activity, indexForum(forumIdx), (threadIdx+1)))
                                        .getAttribute("title"))
                                        , currentThreadUrl));
                    }

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
            }

            outputWriter.flush();
            writer = new OutputStreamWriter(
                    new FileOutputStream(c.folderPath() + "dateFactory.txt"), StandardCharsets.UTF_8);
            writer.write(""+dtf.format(LocalDateTime.now())+"");
            System.out.println("Fetch end; File datetime updated to "+dtf.format(LocalDateTime.now()));
        } catch (Exception e) {
            e.printStackTrace();
            map.clear();
            map.put("error", String.valueOf(e));
            template.process(map, outputWriter);
            System.out.println("Ex thrown: "+e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
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