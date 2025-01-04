
package com.web.appts.repositories;

import com.web.appts.entities.Order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;

public interface OrderRepo extends JpaRepository<Order, Integer> {
	List<Order> findByUser(String paramString);

	List<Order> findByOrderNumber(String paramString);

	@Modifying
	@Query("UPDATE Order o SET o.tra = :newValue WHERE o.id IN :ids")
	int updateFieldForRIds(@Param("newValue") String newValue, @Param("ids") List<Integer> ids);

	@Modifying
	@Query("UPDATE OrderDepartment od SET od.status = :newValue, od.prevStatus = :newValue2 WHERE od.order.id IN :ids AND od.depName = 'TRA'")
	int updateOrderDepartmentStatusR(@Param("newValue") String newValue, @Param("newValue2") String newValue2, @Param("ids") List<Integer> ids);

	@Modifying
	@Query("UPDATE Order o SET o.tra = :newValue WHERE o.id IN :ids")
	int updateFieldForYIds(@Param("newValue") String newValue, @Param("ids") List<Integer> ids);

	@Modifying
	@Query("UPDATE OrderDepartment od SET od.status = :newValue, od.prevStatus = :newValue2 WHERE od.order.id IN :ids AND od.depName = 'TRA'")
	int updateOrderDepartmentStatusY(@Param("newValue") String newValue, @Param("newValue2") String newValue2, @Param("ids") List<Integer> ids);

	@Modifying
	@Transactional
	@Query("DELETE OrderDepartment od WHERE od.order.id IN :ids")
	int deleteODForIds(@Param("ids") List<Integer> ids);

	@Modifying
	@Transactional
	@Query("DELETE MonSubOrders mso WHERE mso.order.id IN :ids")
	int deleteMonSubsForIds(@Param("ids") List<Integer> ids);

	@Modifying
	@Transactional
	@Query("DELETE FROM Order o WHERE o.id IN :ids")
	int deleteOrdersByIds(@Param("ids") List<Integer> ids);

	@Modifying
	@Query("UPDATE Order o SET o.monLb = :newValue WHERE o.id IN :ids")
	int updateFieldForIdsMainmonLb(@Param("newValue") String newValue, @Param("ids") List<Integer> ids);

	@Modifying
	@Query("UPDATE Order o SET o.monTr = :newValue WHERE o.id IN :ids")
	int updateFieldForIdsMainmonTr(@Param("newValue") String newValue, @Param("ids") List<Integer> ids);

	@Modifying
	@Query("UPDATE Order o SET o.mwe = :newValue WHERE o.id IN :ids")
	int updateFieldForIdsMainmwe(@Param("newValue") String newValue, @Param("ids") List<Integer> ids);

	@Modifying
	@Query("UPDATE Order o SET o.ser = :newValue WHERE o.id IN :ids")
	int updateFieldForIdsMainser(@Param("newValue") String newValue, @Param("ids") List<Integer> ids);

	@Modifying
	@Query("UPDATE Order o SET o.exp = :newValue WHERE o.id IN :ids")
	int updateFieldForIdsMainexp(@Param("newValue") String newValue, @Param("ids") List<Integer> ids);

	@Modifying
	@Query("UPDATE OrderDepartment od SET od.status = :newValue, od.prevStatus = :newValue2 WHERE od.order.id IN :ids AND od.depName = :newValue3")
	int updateOrderDepartmentStatusMain(@Param("newValue") String newValue, @Param("newValue2") String newValue2, @Param("newValue3") String newValue3, @Param("ids") List<Integer> ids);
}
