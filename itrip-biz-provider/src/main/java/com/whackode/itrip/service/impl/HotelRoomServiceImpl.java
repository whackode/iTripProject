package com.whackode.itrip.service.impl;

import com.whackode.itrip.dao.HotelOrderDao;
import com.whackode.itrip.dao.HotelRoomDao;
import com.whackode.itrip.pojo.entity.HotelRoom;
import com.whackode.itrip.pojo.vo.SearchHotelRoomVO;
import com.whackode.itrip.service.HotelRoomService;
import jdk.nashorn.internal.runtime.StoredScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.Store;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>爱旅行-酒店房间模块业务层接口实现类</b>
 * @author Arthur
 * @version 1.0.0
 * @since 1.0.0
 */
@Service("hotelRoomService")
@Transactional
public class HotelRoomServiceImpl implements HotelRoomService {
	@Autowired
	private HotelRoomDao hotelRoomDao;
	@Autowired
	private HotelOrderDao hotelOrderDao;

	/**
	 * <b>查询酒店房间列表-此刻可以预定的房间列表</b>
	 * @param searchHotelRoomVO
	 * @return
	 * @throws Exception
	 */
	public List<HotelRoom> queryHotelRoomByHotel(SearchHotelRoomVO searchHotelRoomVO) throws Exception {
		List<HotelRoom> resultList = new ArrayList<HotelRoom>();
		// 根据酒店id查询该酒店所有房间列表
		HotelRoom query = new HotelRoom();
		query.setHotelId(searchHotelRoomVO.getHotelId());
		List<HotelRoom> hotelRoomList = hotelRoomDao.findListByQuery(query);

		if (hotelRoomList != null && hotelRoomList.size() > 0) {
			for (HotelRoom hotelRoom : hotelRoomList) {
				// 循环遍历该列表，根据房间id和当前时间查询临时库存数量
				// 封装查询参数
				Map<String, Object> queryMap = new HashMap<String, Object>();
				queryMap.put("roomId", hotelRoom.getId());
				queryMap.put("beginDate", searchHotelRoomVO.getStartDate());
				Integer store = hotelRoomDao.queryTempStore(queryMap);

				if (store == null) {
					// 如果临时库存不存在，查询总库存数量
					queryMap.put("productId", hotelRoom.getId());
					store = hotelRoomDao.queryTotalStore(queryMap);
				}
				// 计算可用库存，如果库存大于0
				if (store != null && store > 0) {
					// 查询此时该房间订单表中处于未支付和支付成功的订单数量
					Map<String, Object> orderQueryMap = new HashMap<String, Object>();
					orderQueryMap.put("roomId", hotelRoom.getId());
					orderQueryMap.put("startDate", searchHotelRoomVO.getStartDate());
					orderQueryMap.put("endDate", searchHotelRoomVO.getEndDate());
					Integer orderRoomCount = hotelOrderDao.findOrderRoomCountByQuery(orderQueryMap);
					// 使用库存-订单输入，如果大于0则说明该房间可用，那么加入最终的结果列表
					if (store - orderRoomCount > 0) {
						resultList.add(hotelRoom);
					}
				}
			}
		}
		return resultList;
	}
}