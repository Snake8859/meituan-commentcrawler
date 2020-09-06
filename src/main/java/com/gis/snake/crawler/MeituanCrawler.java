package com.gis.snake.crawler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.gis.snake.pojo.IPList;
import com.gis.snake.pojo.TbScenicReviewInfo;
import com.vdurmont.emoji.EmojiParser;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 美团评论爬虫工具类
 */
public class MeituanCrawler {

    private static CloseableHttpClient httpClient =null;
    private static CloseableHttpResponse response = null;
    private static HttpGet httpGet = null;
    private static HttpHost proxy =null;



    /**
     * 景区评论爬虫 - 美团评论
     *      防反爬虫方式：ip代理+降速
     * @param sid
     * @param meituanId
     * @return
     */
    public static List<TbScenicReviewInfo> ScenicReviewInfoCrawler_ProxyIPWithLowSpeed(String sid, String meituanId,String hostName,Integer port) {
        Integer offset =0;
        //url
        String url = "";
        //httpClient
        httpClient = HttpClients.createDefault();
        //proxy
        proxy = new HttpHost(hostName,port);
        RequestConfig requestConfig = RequestConfig.custom().setProxy(proxy).build();
        //评论信息集合
        List<TbScenicReviewInfo> list = new ArrayList<TbScenicReviewInfo>();
        while (true) {
                try {
                    Thread.sleep(1000); //1秒10条
                    url = "https://www.meituan.com/ptapi/poi/getcomment?id=" + meituanId + "&offset=" + offset + "&pageSize=10&mode=0&sortType=1";
                    //构造httpClient和httpGet对象
                    httpGet = new HttpGet(url);
                    //设置ip代理
                    httpGet.setConfig(requestConfig);
                    //请求头设置
                    httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36");
                    //发起请求
                    response = httpClient.execute(httpGet);
                    //获取返回结果
                    if (response.getStatusLine().getStatusCode() == 200) {
                        String content = EntityUtils.toString(response.getEntity(), "utf-8");
                        //获得总评论数
                        Integer total = (Integer) JSON.parseObject(content).get("total");
                        //获得评论JSON对象
                        JSONArray comments = (JSONArray) JSON.parseObject(content).get("comments");
                        if (comments == null) {
                            System.out.println("爬虫结束");
                            break;
                        }
                        for (int i = 0; i < comments.size(); i++) { //解析评论
                            try {
                                TbScenicReviewInfo tbScenicReviewInfo = new TbScenicReviewInfo();
                                //评论id
                                tbScenicReviewInfo.setReviewId(comments.getJSONObject(i).getString("reviewId"));
                                //评论人
                                tbScenicReviewInfo.setAuthor(comments.getJSONObject(i).getString("userName"));
                                //评论人照片
                                tbScenicReviewInfo.setAuthorProfileUrl(comments.getJSONObject(i).getString("userUrl"));
                                //评论内容照片
                                tbScenicReviewInfo.setPicInfo(comments.getJSONObject(i).getString("picUrls"));
                                //评论内容
                                tbScenicReviewInfo.setReview(EmojiParser.replaceAllEmojis((comments.getJSONObject(i).getString("comment")), "-"));
                                //评论得分
                                tbScenicReviewInfo.setScore(Integer.parseInt(comments.getJSONObject(i).getString("star")) / 10);
                                //评论来源
                                tbScenicReviewInfo.setSrcName("美团网");
                                //评论时间
                                tbScenicReviewInfo.setTime(timeStamp2Date(comments.getJSONObject(i).getString("commentTime")));
                                //景区id
                                tbScenicReviewInfo.setSid(sid);
                                list.add(tbScenicReviewInfo);
                            } catch (Exception e) {
                                System.out.println("数据清洗异常");
                                e.printStackTrace();
                                continue;
                            }
                            //System.out.println(tbScenicReviewInfo);
                        }
                        System.out.println("美团评论爬虫已爬取:" + list.size() + "条评论");
                    } else { //访问过于频繁
                        System.out.println("403：访问过于频繁");
                        System.out.println("重新校验ip,切换ip");
                        //清除原先的ip
                        IPList.clearList();
                        //重新校验ip
                        verifyIP();
                        proxy = new HttpHost(IPList.getIpBeanList().get(0).split(":")[0],Integer.parseInt(IPList.getIpBeanList().get(0).split(":")[1]));
                        requestConfig = RequestConfig.custom().setProxy(proxy).build();
                        continue;
                    }
                    //偏移量递增10
                    offset += 10;
            }catch (Exception e){
                    //e.printStackTrace();
                    System.out.println("get访问异常");
                    System.out.println("重新校验ip,切换ip");
                    //清除原先的ip
                    IPList.clearList();
                    //重新校验ip
                    verifyIP();
                    proxy = new HttpHost(IPList.getIpBeanList().get(0).split(":")[0],Integer.parseInt(IPList.getIpBeanList().get(0).split(":")[1]));
                    requestConfig = RequestConfig.custom().setProxy(proxy).build();
                    continue;
                }
        }
        return list;

    }


    /**
     * 景区评论爬虫 - 美团评论
     *      防反爬虫方式：降速
     * @param sid
     * @param meituanId
     * @return
     */
    public static List<TbScenicReviewInfo> ScenicReviewInfoCrawler_LowSpeed(String sid, String meituanId) {
        Integer offset =0;
        //url
        String url = "";
        //httpClient
        httpClient = HttpClients.createDefault();
        //评论信息集合
        List<TbScenicReviewInfo> list = new ArrayList<TbScenicReviewInfo>();
        try {
            while (true){
                Thread.sleep(1000*5); //休眠5秒
                url = "https://www.meituan.com/ptapi/poi/getcomment?id="+meituanId+"&offset="+offset+"&pageSize=10&mode=0&sortType=1";
                //构造httpClient和httpGet对象
                httpGet = new HttpGet(url);
                //请求头设置
                httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36");
                //发起请求
                response =  httpClient.execute(httpGet);
                //获取返回结果
                if (response.getStatusLine().getStatusCode() == 200) {
                    String content = EntityUtils.toString(response.getEntity(), "utf-8");
                    //获得总评论数
                    Integer total = (Integer) JSON.parseObject(content).get("total");
                    //获得评论JSON对象
                    JSONArray comments = (JSONArray) JSON.parseObject(content).get("comments");
                    if(comments==null){
                        System.out.println("爬虫结束");
                        break;
                    }
                    for (int i = 0; i < comments.size(); i++) { //解析评论
                        try {
                            TbScenicReviewInfo tbScenicReviewInfo = new TbScenicReviewInfo();
                            //评论id
                            tbScenicReviewInfo.setReviewId(comments.getJSONObject(i).getString("reviewId"));
                            //评论人
                            tbScenicReviewInfo.setAuthor(comments.getJSONObject(i).getString("userName"));
                            //评论人照片
                            tbScenicReviewInfo.setAuthorProfileUrl(comments.getJSONObject(i).getString("userUrl"));
                            //评论内容照片
                            tbScenicReviewInfo.setPicInfo(comments.getJSONObject(i).getString("picUrls"));
                            //评论内容
                            tbScenicReviewInfo.setReview(EmojiParser.replaceAllEmojis((comments.getJSONObject(i).getString("comment")),"-"));
                            //评论得分
                            tbScenicReviewInfo.setScore(Integer.parseInt(comments.getJSONObject(i).getString("star")) / 10);
                            //评论来源
                            tbScenicReviewInfo.setSrcName("美团网");
                            //评论时间
                            tbScenicReviewInfo.setTime(timeStamp2Date(comments.getJSONObject(i).getString("commentTime")));
                            //景区id
                            tbScenicReviewInfo.setSid(sid);
                            list.add(tbScenicReviewInfo);
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            continue;
                        }
                        //System.out.println(tbScenicReviewInfo);
                        System.out.println("美团评论爬虫已爬取:"+(offset+1)*10+"条评论");
                    }
                    //偏移量递增10
                    offset += 10;
                }else { //访问过于频繁，已被禁止
                    System.out.println("403：访问过于频繁");
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 13位时间戳转化年月日
     * @param time
     * @return
     */
    public static String timeStamp2Date(String time) {
        Long timeLong = Long.parseLong(time);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");//要转换的时间格式
        Date date;
        try {
            date = sdf.parse(sdf.format(timeLong));
            return sdf.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取ip池
     * @return
     */
    public static  List<String> getIPPool()  {
        String fileName = System.getProperty("user.dir")+"\\src\\main\\resources\\static\\ip池.txt";
        //读取文件
        List<String> lineLists = null;
        try {
            System.out.println(fileName);
            lineLists = Files
                    .lines(Paths.get(fileName), Charset.defaultCharset())
                    .flatMap(line -> Arrays.stream(line.split("\n")))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  lineLists;
    }

    /**
     * 过滤ip
     * @param hostName
     * @param port
     * @return
     */
    public static boolean filterIPBean(String hostName,Integer port){
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostName, port));
        try {
            URLConnection httpCon = new URL("https://www.baidu.com/").openConnection(proxy);
            httpCon.setConnectTimeout(5000);
            httpCon.setReadTimeout(5000);
            int code = ((HttpURLConnection) httpCon).getResponseCode();
            if(code==200){
                System.out.println(hostName+":"+port+" 有效");
                return true;
            }
        } catch (IOException e) {
            //e.printStackTrace();
            //System.out.println(hostName+":"+port+" 失效");
        }
        return false;
    }

    /**
     * ip校验
     */
    public  static void   verifyIP(){
        //拿到所有免费ip代理
        List<String> ipPool = getIPPool();
        //校验ip代理
        for (String ip: ipPool) {
            String hostName = ip.split(":")[0];
            Integer port = Integer.parseInt(ip.split(":")[1]);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        boolean flag = filterIPBean(hostName,port);
                        if (flag) {
                            //若ip有效
                            IPList.add(hostName,port);
                        }
                        IPList.increase();
                    }catch (Exception e){
                        //System.out.println("异常");
                        //e.printStackTrace();
                        IPList.increase();
                    }

                }
            }).start();
        }
        while (true) {
            // 判断所有副线程是否完成
            if (IPList.getCount() == ipPool.size()) {
                System.out.println("有效数量：" + IPList.getSize());
                break;
            }
        }
    }

    public static void main(String[] args) {
        verifyIP();
        String hostName = IPList.getIpBeanList().get(0).split(":")[0];
        Integer port = Integer.parseInt(IPList.getIpBeanList().get(0).split(":")[1]);
        System.out.println(ScenicReviewInfoCrawler_ProxyIPWithLowSpeed("B0FFH0MNUA","4719888",hostName,port).size());
    }


}
