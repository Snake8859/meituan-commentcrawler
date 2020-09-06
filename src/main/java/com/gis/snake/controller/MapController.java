package com.gis.snake.controller;

import com.gis.snake.crawler.MeituanCrawler;
import com.gis.snake.mapper.MapMapper;
import com.gis.snake.pojo.IPList;
import com.gis.snake.pojo.TbScenicReviewInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 美团评论爬虫 - 控制层
 */
@RestController
@RequestMapping("/map")
public class MapController {

    @Autowired
    private MapMapper mapMapper;

    @RequestMapping("/meituanScenicReviewInfoCrawler1")
    public void meituanScenicReviewInfoCrawler1(@RequestParam(value = "sid",required = true)String sid,
                                                @RequestParam(value = "meituanId",required = true) String meituanId){
        //先清空ip池
        IPList.clearList();
        //校验id
        MeituanCrawler.verifyIP();
        //获取有效的ip
        String hostName = IPList.getIpBeanList().get(0).split(":")[0];
        Integer port = Integer.parseInt(IPList.getIpBeanList().get(0).split(":")[1]);
        //爬虫爬取美团评论
        List<TbScenicReviewInfo> list = MeituanCrawler.ScenicReviewInfoCrawler_ProxyIPWithLowSpeed(sid,meituanId,hostName,port);
        Integer i = 0;
        for (TbScenicReviewInfo tbScenicReviewInfo:list) {
            try {
                mapMapper.insertScenicReviewInfo(tbScenicReviewInfo);
            }
            catch (Exception e){
                e.printStackTrace();
                i++;
                continue;
            }
        }
        System.out.println("插入失败条数:"+i);
        System.out.println("入库结束");
    }

}
