package com.miaoshaproject.controller;

import com.miaoshaproject.controller.viewobject.ItemVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.model.ItemModel;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller("item")
@RequestMapping("/item")
@CrossOrigin(allowCredentials="true",allowedHeaders="*")
public class ItemController extends BaseController {
    @Autowired
    private ItemService itemService;

    //创建商品的controller
    @RequestMapping(value = "/create",method = {RequestMethod.POST},consumes ={CONTENT_TYPE_FORMED} )
    @ResponseBody
    public CommonReturnType createItem(@RequestParam(name = "title")String title,
                                       @RequestParam(name = "description")String description,
                                       @RequestParam(name = "imgUrl")String imgUrl,
                                       @RequestParam(name = "price")BigDecimal price,
                                       @RequestParam(name = "stock")Integer stock) throws BusinessException {
        //封装service请求用来创建商品
        ItemModel itemModel=new ItemModel();
        itemModel.setTitle(title);
        itemModel.setPrice(price);
        itemModel.setStock(stock);
        itemModel.setDescription(description);
        itemModel.setImgUrl(imgUrl);

        ItemModel itemModelForReturn=itemService.createItem(itemModel);
        ItemVO itemVO=convertItemVOFromModel(itemModelForReturn);
        return CommonReturnType.creat(itemVO);

    }

    //商品详情页浏览
    @RequestMapping(value = "/get",method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getItem(@RequestParam(name = "id")Integer id){
        ItemModel itemModel=itemService.getItemById(Integer.valueOf(id));

        ItemVO itemVO=convertItemVOFromModel(itemModel);

        return CommonReturnType.creat(itemVO);
    }

    //商品列表浏览
    @RequestMapping(value = "/list",method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType listItem(){
        List<ItemModel> itemModelList=itemService.listItem();
        List<ItemVO>itemVOList=itemModelList.stream().map(itemModel -> {
            ItemVO itemVO=convertItemVOFromModel(itemModel);
            return itemVO;
        }).collect(Collectors.toList());
        return CommonReturnType.creat(itemVOList);
    }



    private ItemVO convertItemVOFromModel(ItemModel itemModel){
        if(itemModel==null)
            return null;
        ItemVO itemVO=new ItemVO();
        BeanUtils.copyProperties(itemModel,itemVO);
        if(itemModel.getPromoModel()!=null){
            //有正在进行或即将进行的秒杀活动
            itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setPromoItemPrice(itemModel.getPromoModel().getPromoItemPrice());
            itemVO.setStartTime(itemModel.getPromoModel().getStartTime().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
        }else{
            itemVO.setPromoStatus(0);
        }
        return itemVO;
    }

}
