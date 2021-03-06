package com.jojo.pad.ui.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.jojo.pad.R;
import com.jojo.pad.base.BaseAcitivty;
import com.jojo.pad.constant.Constant;
import com.jojo.pad.constant.HttpConstant;
import com.jojo.pad.listener.ResponseListener;
import com.jojo.pad.listener.ViewClickListener;
import com.jojo.pad.model.bean.OrderBean;
import com.jojo.pad.model.bean.SaleBean;
import com.jojo.pad.model.bean.print.PrintGoodBean;
import com.jojo.pad.model.bean.result.SaleAddResultBean;
import com.jojo.pad.model.http.BaseHttp;
import com.jojo.pad.print.UsbPrinter;
import com.jojo.pad.showprice.EpsonPosPrinterCommand;
import com.jojo.pad.showprice.PriceShowUtil;
import com.jojo.pad.util.AccountUtil;
import com.jojo.pad.util.Convert;
import com.jojo.pad.util.USBPrinterUtil;
import com.jojo.pad.util.ThreadPoolManager;
import com.jojo.pad.widget.CheckOutRoot;
import com.jojo.pad.widget.DiscountSelectView;
import com.jojo.pad.widget.PadHeader;
import com.kongqw.serialportlibrary.Device;
import com.kongqw.serialportlibrary.SerialPortManager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CheckOutActivity extends BaseAcitivty implements View.OnClickListener {
    @BindView(R.id.tv_discount_number)
    TextView tvDiscountNumber;
    @BindView(R.id.tv_discount)
    TextView tvDiscount;
    @BindView(R.id.tv_cash)
    RadioButton tvCash;
    @BindView(R.id.tv_weixin)
    RadioButton tvWeixin;
    @BindView(R.id.tv_zhifubao)
    RadioButton tvZhifubao;
    @BindView(R.id.tv_card)
    RadioButton tvCard;
    @BindView(R.id.tv_bank)
    RadioButton tvBank;
    @BindView(R.id.gadiogroup)
    RadioGroup gadiogroup;
    Button btnDetail;
    @BindView(R.id.tv_confirm)
    Button tvConfirm;
    @BindView(R.id.tv_discount_title)
    TextView tvDiscountTitle;
    @BindView(R.id.tv_discount_count)
    TextView tvDiscountCount;
    @BindView(R.id.tv_documents)
    CheckBox tvDocuments;
    @BindView(R.id.checkout)
    CheckOutRoot checkout;
    @BindView(R.id.tv_order_discount)
    TextView tvOrderDiscount;
    @BindView(R.id.tv_order_sum)
    TextView tvOrderSum;
    @BindView(R.id.tv_truenleft)
    TextView tvTruenleft;
    @BindView(R.id.tv_more)
    TextView tvMore;
    @BindView(R.id.ll_discount)
    LinearLayout llDiscount;
    @BindView(R.id.ll_discount_item)
    LinearLayout llDiscountItem;


    @BindView(R.id.tv_pay_type)
    TextView tvPayType;
    @BindView(R.id.tv_pay_end)
    TextView tvPayEnd;
    @BindView(R.id.tv_repay_money)
    TextView tvRepayMoney;
    @BindView(R.id.ll_root)
    LinearLayout llRoot;
    @BindView(R.id.header)
    PadHeader header;

    private PopupWindow mPopWindow;
    private DiscountSelectView discountSelect;
    private boolean isfirstshowdiscount = true;

    private List<OrderBean> datas;
    private double sum = 0;//应收金额
    private int cousts = 0;//总数量
    private String cid;
    private int paytype = 2;//0货到付款 1微信支付 2现金 3银行卡 4支付宝 5储值卡 6欠款
    private List<SaleBean.Data> saleBeanList;
    private double end = 0;

    private UsbPrinter usbprint;

    private SaleBean saleBean;
    private int discount = 10;//整单折扣
    private double discountend;//折后金额


    //价格金额显示
    private SerialPortManager mSerialPortManager;
    @Override
    public int getLayoutId() {
        return R.layout.activity_check_outctivity;
    }

    @Override
    public void initView() {
        datas = (ArrayList<OrderBean>) getIntent().getSerializableExtra("orders");
        saleBeanList = new ArrayList<>();
        saleBean = new SaleBean();
        if (getIntent().hasExtra("cid")) {
            cid = getIntent().getStringExtra("cid");
        }
        if (datas != null) {
            for (OrderBean orderBean : datas) {
                sum += Double.parseDouble(orderBean.getGoods_price()) * orderBean.getCount();
                cousts += orderBean.getCount();
                SaleBean.Data sale = new SaleBean.Data();
                sale.setGid(orderBean.getGid());
                sale.setDiscount(10);
                sale.setGoods_number(orderBean.getCount() + "");
                sale.setMsg("");
                saleBeanList.add(sale);
            }
            discountend = sum;
            saleBean.setSale_list(saleBeanList);
            tvOrderSum.setText("￥"+sum);
            tvPayEnd.setText("￥"+sum );
            tvOrderDiscount.setText("￥"+sum );
            tvDiscountCount.setText("100%");
        }

        initPrinter();
        initPort();
        //收款
        mSerialPortManager.sendBytes(PriceShowUtil.getShowByte(sum+"",PriceShowUtil.DISPLAY_STATE_AMOUNT));
    }

    private void initPrinter() {
        usbprint = UsbPrinter.getInstance();
        usbprint.init(this);
    }
    //初始化客显端口
    private void initPort() {
        Device device = PriceShowUtil.getDevice();
        mSerialPortManager = new SerialPortManager();
        if (device != null){
            mSerialPortManager.openSerialPort(device.getFile(), 2400);
        }
        mSerialPortManager.sendBytes(EpsonPosPrinterCommand.ESC_INIT);
    }

    @Override
    public void setListener() {
        checkout.setSearchListener(new ViewClickListener() {
            @Override
            public void clickListener(String msg, int type) {
                if (type == Constant.VIEW_CLICK_TYPE_NUMBER) {
                    if (!TextUtils.isEmpty(msg)) {
                        tvPayEnd.setText(msg);
                        double payend = Double.parseDouble(msg);
                        end = payend - sum;
                        tvRepayMoney.setText("" + end);
                        //显示找零
                        mSerialPortManager.sendBytes(PriceShowUtil.getShowByte(end+"",PriceShowUtil.DISPLAY_STATE_CHAGNE));
                    }
                } else if (type == Constant.VIEW_CLICK_TYPE_COMFIRM) {
                    if (end >= 0) {
                        saleOrder();
                    }
                }
            }
        });


        tvTruenleft.setOnClickListener(this);
        tvDiscountNumber.setOnClickListener(this);
        tvDiscount.setOnClickListener(this);
        tvMore.setOnClickListener(this);
        llRoot.setOnClickListener(this);
        header.setBackClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        gadiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {

                    case R.id.tv_weixin:
                        paytype = 1;
                        tvPayType.setText("微信");
                        break;
                    case R.id.tv_cash:
                        paytype = 2;
                        tvPayType.setText("现金");
                        break;
                    case R.id.tv_zhifubao:
                        paytype = 4;
                        tvPayType.setText("支付宝");
                        break;
                    case R.id.tv_bank:
                        paytype = 3;
                        tvPayType.setText("银行卡");
                        break;
                    case R.id.tv_card:
                        paytype = 5;
                        tvPayType.setText("储值卡");
                        break;
                    default:
                        paytype = 2;
                        break;
                }
            }
        });
    }


    @Override
    public void initData() {

    }

    @Override
    public void onClick(View v) {
        if (mPopWindow != null && mPopWindow.isShowing()) {
            mPopWindow.dismiss();
        }
        switch (v.getId()) {
            case R.id.ll_root:
                break;
            case R.id.tv_truenleft:
                llDiscount.setVisibility(View.GONE);
                llDiscountItem.setVisibility(View.VISIBLE);
                break;
            case R.id.tv_discount_number:
                tvDiscountTitle.setText("优惠券码:");
                llDiscountItem.setVisibility(View.GONE);
                llDiscount.setVisibility(View.VISIBLE);

                break;
            case R.id.tv_discount:
                tvDiscountTitle.setText("折扣率");
                llDiscountItem.setVisibility(View.GONE);
                llDiscount.setVisibility(View.VISIBLE);
                showPopupWindow();
                break;
            case R.id.tv_more:
                showPopupWindow();
                break;
            default:
                break;
        }

    }


    private void showPopupWindow() {

        if (mPopWindow == null) {
            View contentView = LayoutInflater.from(mContext).inflate(R.layout.pop_discount_layout, null);
            mPopWindow = new PopupWindow(contentView);
            mPopWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
            mPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);

            discountSelect = contentView.findViewById(R.id.discount_select);
            discountSelect.setViewClickListener(new ViewClickListener() {
                @Override
                public void clickListener(String msg, int type) {
                    mPopWindow.dismiss();
                    if (type == Constant.VIEW_CLICK_TYPE_NUMBER) {
                        tvDiscountCount.setText(msg+"%");
                         discount = Integer.parseInt(msg);

                         discountend = sum * discount /100;
                        tvOrderDiscount.setText("￥"+discountend);
                        tvPayEnd.setText("￥"+discountend);


                    } else if (type == Constant.VIEW_CLICK_TYPE_COMFIRM) {
                        int showend = (int) (discountend = (int)discountend);

                        tvOrderDiscount.setText("￥"+showend);
                        tvPayEnd.setText("￥"+showend);
                        LogUtils.e(discountend);
                    }

                }
            });
        }

        if (isfirstshowdiscount) {
            isfirstshowdiscount = false;
            tvDiscountCount.getHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPopWindow.showAsDropDown(tvDiscountCount);
                }
            }, 800);
        } else {
            mPopWindow.showAsDropDown(tvDiscountCount);
        }
    }

    private void saleOrder() {
        saleBean.setStore_id(AccountUtil.store_id);
        saleBean.setUser_id(AccountUtil.user_id);
        saleBean.setType(paytype);
        if (!TextUtils.isEmpty(cid)) {
            saleBean.setCid(cid);
        }
        BaseHttp.postJson(HttpConstant.Api.saleAdd, Convert.toJson(saleBean), activity, new ResponseListener() {
            @Override
            public void onSuccess(Object result) {
                if (tvDocuments.isChecked()) {
                    final SaleAddResultBean resultBean = Convert.fromJObject(result, SaleAddResultBean.class);

                    final PrintGoodBean printGoodBean = new PrintGoodBean();
                    printGoodBean.setCount(cousts);
                    printGoodBean.setSum(sum);
                    printGoodBean.setOrder_id(resultBean.getOrder_id());
                    printGoodBean.setPay_type_name(resultBean.getPay_type_name());
                    printGoodBean.setOrder_id(resultBean.getOrder_id());
                    printGoodBean.setReal_money(resultBean.getReal_money());
                    printGoodBean.setDatas(datas);


                    ThreadPoolManager.newInstance().addExecuteTask(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                usbprint.sendMessageToPoint(USBPrinterUtil.printSaleOrder(printGoodBean));
                            } catch (Exception e) {
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    LogUtils.e("usbprint");
                                    setResult(RESULT_OK);
                                    finish();
                                }
                            });
                        }
                    });
                } else {
                    setResult(RESULT_OK);
                    finish();
                }
            }

            @Override
            public void onError(String result) {
                ToastUtils.showShort(result);
            }
        });


    }

    @Override
    protected void onDestroy() {
        if (null != mSerialPortManager) {
            mSerialPortManager.closeSerialPort();
            mSerialPortManager = null;
        }
        super.onDestroy();
        usbprint.onDestory();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: add setContentView(...) invocation
        ButterKnife.bind(this);
    }
}
