package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.OrderDOMapper;
import com.miaoshaproject.dao.SequenceDOMapper;
import com.miaoshaproject.dataobject.OrderDO;
import com.miaoshaproject.dataobject.SequenceDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.OrderModel;
import com.miaoshaproject.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderDOMapper orderDOMapper;

    @Autowired
    private SequenceDOMapper sequenceDOMapper;

    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId,Integer promoId, Integer amount) throws BusinessException {
        //1.校验下单状态，下单的商品是否存在，用户是否合法，购买数量是否正确
        ItemModel itemModel=itemService.getItemById(itemId);
        if(itemModel==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"商品信息不存在");
        }
        UserModel userModel=userService.getUserById(userId);
        if(userModel==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"用户信息不存在");
        }
        if(amount<=0||amount>=99){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"数量信息不正确");
        }
        //校验活动信息
        if(promoId!=null){
            //1.校验对应活动是否存在这个适用商品
            if(promoId!=itemModel.getPromoModel().getId()){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动信息不正确");
            }//校验活动是否正在进行中
            else if(itemModel.getPromoModel().getStatus()!=2){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动还未开始");
            }
        }
        //2.落单减库存
        boolean result=itemService.decreaseStock(itemId,amount);
        if(!result){
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }

        //3.订单入库
        OrderModel orderModel=new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setAmount(amount);
        orderModel.setItemId(itemId);
        orderModel.setPromoId(promoId);
        if(promoId!=null){
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        } else {
            orderModel.setItemPrice(itemModel.getPrice());
        }
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));
        //生成交易订单号，即id
        orderModel.setId(generateOrderNo());
        OrderDO orderDO=convertFromOrderModel(orderModel);
        orderDOMapper.insertSelective(orderDO);

        //加上商品的销量
        itemService.increaseSales(itemId,amount);

        //返回前端
        return orderModel;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    String generateOrderNo(){
        //订单号有16位
        StringBuilder stringBuilder=new StringBuilder();
        //前8位为时间信息
        LocalDateTime now=LocalDateTime.now();
        String nowDate=now.format(DateTimeFormatter.ISO_DATE).replace("-","");
        stringBuilder.append(nowDate);

        //中间6位为自增序列
        int sequence=0;
        SequenceDO sequenceDO=sequenceDOMapper.getSequenceByName("order_info");
        sequence=sequenceDO.getCurentValue();
        sequenceDO.setCurentValue(sequenceDO.getCurentValue()+sequenceDO.getStop());
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);
        String sequenceStr=String.valueOf(sequence);
        for(int i=0;i<6-sequenceStr.length();i++){
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);

        //最后2位为分库分表位
        stringBuilder.append("00");
        return stringBuilder.toString();
    }

    private OrderDO convertFromOrderModel(OrderModel orderModel){
        if(orderModel==null)
            return  null;
        OrderDO orderDO=new OrderDO();
        BeanUtils.copyProperties(orderModel,orderDO);
        return orderDO;
    }
}
