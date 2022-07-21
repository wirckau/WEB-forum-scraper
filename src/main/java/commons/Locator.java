package pckge.commons;

import org.openqa.selenium.By;

public class Locator {
    //wrappers
    public static By xPath(String locator, int no) {
        return By.xpath(String.format(locator, no));
    }
    public static By xPath(String locator, String param1, int no) {
        return By.xpath(String.format(locator, param1, no));
    }
}