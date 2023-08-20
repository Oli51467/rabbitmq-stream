package com.sdu.rabbitmq.settlement.listener;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.common.commons.enums.OrderStatus;
import com.sdu.rabbitmq.common.domain.dto.OrderMessageDTO;
import com.sdu.rabbitmq.common.domain.po.OrderDetail;
import com.sdu.rabbitmq.common.domain.po.ProductOrderDetail;
import com.sdu.rabbitmq.rdts.listener.AbstractDlxListener;
import com.sdu.rabbitmq.settlement.repository.OrderDetailMapper;
import com.sdu.rabbitmq.settlement.repository.ProductMapper;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Component
public class DlxListener extends AbstractDlxListener {

    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Resource
    private ProductMapper productMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean receiveMessage(Message message) throws IOException {
        OrderMessageDTO orderMessage = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);
        // 将该订单关闭
        UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", orderMessage.getOrderId()).set("status", OrderStatus.FAILED.toString());
        orderDetailMapper.update(null, updateWrapper);
        // 查询该订单的中所有商品及商品的数量
        for (ProductOrderDetail productOrderDetail : orderMessage.getDetails()) {
            productMapper.unlockStock(productOrderDetail.getProductId(), productOrderDetail.getCount());
        }
        return true;
    }
}


