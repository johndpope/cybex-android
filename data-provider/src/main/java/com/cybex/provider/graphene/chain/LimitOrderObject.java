package com.cybex.provider.graphene.chain;

import java.io.Serializable;
import java.util.Date;

public class LimitOrderObject implements Serializable {

    public ObjectId<LimitOrderObject> id;
    public Date expiration;
    public ObjectId<AccountObject> seller;
    //
    public long for_sale; ///< Asset id is sell_price.base.asset_id
    public Price sell_price;
    public long deferred_fee;

    public Asset amount_for_sale() {
        return new Asset( for_sale, sell_price.base.asset_id );
    }

    public Asset amount_to_receive() {
        return amount_for_sale().multipy(sell_price);
    }
}
