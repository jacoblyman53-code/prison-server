package com.prison.economy;

/**
 * TransactionType — every IGC and Token movement is tagged with one of these.
 * The string value is stored directly in the transactions.type column.
 */
public enum TransactionType {
    MINE_SELL,
    RANKUP,
    PRESTIGE,
    PAY_SENT,
    PAY_RECEIVED,
    ENCHANT_PURCHASE,
    CRATE_REWARD,
    TOKEN_EARN,
    KIT_CLAIM,
    AUCTION_SALE,
    AUCTION_PURCHASE,
    IGC_SHOP_PURCHASE,
    ADMIN_SET,
    ADMIN_ADD,
    ADMIN_REMOVE,
    GANG_DEPOSIT,
    GANG_WITHDRAW,
    DAILY_REWARD
}
