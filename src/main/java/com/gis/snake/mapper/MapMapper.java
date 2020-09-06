package com.gis.snake.mapper;

import com.gis.snake.pojo.TbScenicReviewInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MapMapper {

    @Insert("insert into tb_scenic_review_info values(#{reviewId},#{author},#{authorProfileUrl},#{picInfo},#{review},#{score},#{srcName},#{time},#{sid})")
    Integer insertScenicReviewInfo(TbScenicReviewInfo tbScenicReviewInfo);

}
