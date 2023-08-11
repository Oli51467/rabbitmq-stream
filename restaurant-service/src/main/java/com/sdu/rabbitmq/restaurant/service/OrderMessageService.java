package com.sdu.rabbitmq.restaurant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.rdts.listener.AbstractMessageListener;
import com.sdu.rabbitmq.rdts.transmitter.TransMessageTransmitter;
import com.sdu.rabbitmq.restaurant.enums.OrderStatus;
import com.sdu.rabbitmq.restaurant.enums.ProductStatus;
import com.sdu.rabbitmq.restaurant.enums.RestaurantStatus;
import com.sdu.rabbitmq.restaurant.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.restaurant.entity.po.Product;
import com.sdu.rabbitmq.restaurant.entity.po.Restaurant;
import com.sdu.rabbitmq.restaurant.repository.ProductMapper;
import com.sdu.rabbitmq.restaurant.repository.RestaurantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.io.IOException;

@Service("OrderMessageService")
@Slf4j
public class OrderMessageService extends AbstractMessageListener {

    ObjectMapper objectMapper = new ObjectMapper();

    @Value("${rabbitmq.exchange.order-restaurant}")
    private String orderRestaurantExchange;

    @Value("${rabbitmq.order-routing-key}")
    private String orderRoutingKey;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private RestaurantMapper restaurantMapper;

    @Resource
    private TransMessageTransmitter transmitter;

    @Override
    public void receiveMessage(Message message) throws IOException {
        String messageBody = new String(message.getBody());
        log.info("接收消息体: {}", messageBody);
        OrderMessageDTO orderMessage = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);
        log.info("当前订单状态: {}", orderMessage.getOrderStatus());
        // 根据产品id从数据库获取到订单中的产品
        Product product = productMapper.selectById(orderMessage.getProductId());
        log.info("订单产品信息: {}", product);
        // 根据产品中的餐厅id获取到餐厅信息
        Restaurant restaurant = restaurantMapper.selectById(product.getRestaurantId());
        log.info("餐厅信息: {}", restaurant);
        // 确定商品没有下架并且餐厅正常营业
        if (product.getStatus() == ProductStatus.AVAILABLE && restaurant.getStatus() == RestaurantStatus.OPEN) {
            orderMessage.setConfirmed(true);
            orderMessage.setPrice(product.getPrice());
        } else {
            orderMessage.setConfirmed(false);
        }
        // 确认无误后，将消息回发给订单服务
        log.info("餐厅服务发送给订单服务: {}", orderMessage);
        transmitter.send(orderRestaurantExchange, orderRoutingKey, orderMessage);
    }
}
