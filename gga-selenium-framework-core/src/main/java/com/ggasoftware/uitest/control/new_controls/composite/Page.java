package com.ggasoftware.uitest.control.new_controls.composite;

import com.ggasoftware.uitest.control.interfaces.base.IComposite;
import com.ggasoftware.uitest.control.new_controls.base.BaseElement;

/**
 * Created by Roman_Iovlev on 7/17/2015.
 */
public class Page extends BaseElement implements IComposite {
    private String url;
    private String title;
    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public void updatePageData(String url, String title) {
        if (this.url == null)
            this.url = url;
        if (this.title == null)
            this.title = title;
    }

    public Page() {}
    public Page(String url) {
        this.url = url;
    }
    public Page(String url, String title) {
        this.url = url;
        this.title = title;
    }

    public void open() { getDriver().navigate().to(url); }
    public static void openUrl(String url) {
        new Page(url).open();
    }
    public void refresh() {
        getDriver().navigate().refresh();
    }
}