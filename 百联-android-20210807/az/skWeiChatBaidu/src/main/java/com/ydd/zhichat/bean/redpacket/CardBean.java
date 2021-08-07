package com.ydd.zhichat.bean.redpacket;

/**
 * Created by phy on 2020/1/3
 */
public class CardBean {

    /**
     * uid : 123sfasf
     * cardName : 白金纪念卡
     * bankBrandId : 102
     * bankBrandName : 中国农业银行
     * cardType : 0
     * id : 02xxxx
     * userName : 姓名
     * cardNo : 23044873134
     * openBankAddr : 北京市xx路
     */

    private String uid;
    private String cardName;
    private int bankBrandId;
    private String bankBrandName;
    private int cardType;
    private String id;
    private String userName;
    private String cardNo;
    private String openBankAddr;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public int getBankBrandId() {
        return bankBrandId;
    }

    public void setBankBrandId(int bankBrandId) {
        this.bankBrandId = bankBrandId;
    }

    public String getBankBrandName() {
        return bankBrandName;
    }

    public void setBankBrandName(String bankBrandName) {
        this.bankBrandName = bankBrandName;
    }

    public int getCardType() {
        return cardType;
    }

    public void setCardType(int cardType) {
        this.cardType = cardType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getOpenBankAddr() {
        return openBankAddr;
    }

    public void setOpenBankAddr(String openBankAddr) {
        this.openBankAddr = openBankAddr;
    }
}
