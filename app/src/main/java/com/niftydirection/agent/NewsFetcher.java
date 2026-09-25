package com.niftydirection.agent;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;

public final class NewsFetcher {
    public static final class NewsItem{public final String title,link,pubDate,source;NewsItem(String t,String l,String p,String s){title=t;link=l;pubDate=p;source=s;}}
    private static final String[] QUERIES={"NIFTY India stock market","India RBI markets rupee inflation","crude oil India stocks","US Fed Treasury yields global markets","Middle East oil markets India","India FII DII NIFTY"};
    private NewsFetcher(){}
    public static List<NewsItem> fetchTopNews()throws Exception{
        List<NewsItem> r=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(String q:QUERIES){if(r.size()>=18)break;String e=URLEncoder.encode(q,StandardCharsets.UTF_8.name());
            HttpURLConnection c=(HttpURLConnection)new URL("https://news.google.com/rss/search?q="+e+"&hl=en-IN&gl=IN&ceid=IN:en").openConnection();
            c.setConnectTimeout(8000);c.setReadTimeout(10000);c.setRequestProperty("User-Agent","NIFTY-Direction-Agent/1.0");
            try(InputStream in=c.getInputStream()){parse(in,r,seen,4);}finally{c.disconnect();}
        }return r;
    }
    private static void parse(InputStream in,List<NewsItem> out,Set<String> seen,int max)throws Exception{
        XmlPullParserFactory f=XmlPullParserFactory.newInstance();f.setNamespaceAware(false);XmlPullParser p=f.newPullParser();
        p.setInput(new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8)));int ev,count=0;String tag=null,title=null,link=null,date=null,source=null;
        while((ev=p.next())!=XmlPullParser.END_DOCUMENT&&count<max){
            if(ev==XmlPullParser.START_TAG){tag=p.getName();if("source".equals(tag)){source=p.nextText();tag=null;}}
            else if(ev==XmlPullParser.TEXT){if("title".equals(tag))title=p.getText();else if("link".equals(tag))link=p.getText();else if("pubDate".equals(tag))date=p.getText();}
            else if(ev==XmlPullParser.END_TAG&&"item".equals(p.getName())){if(title!=null&&!title.trim().isEmpty()&&!seen.contains(title.trim())){seen.add(title.trim());out.add(new NewsItem(title.trim(),link,date,source));count++;}title=link=date=source=null;tag=null;}
        }
    }
}
