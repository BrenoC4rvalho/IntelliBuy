package com.breno.intellibuy.repository;

import com.breno.intellibuy.model.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseItemRepository  extends JpaRepository<PurchaseItem, Long> {
}
