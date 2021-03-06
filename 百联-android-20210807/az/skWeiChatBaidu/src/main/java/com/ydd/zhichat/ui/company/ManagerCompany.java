package com.ydd.zhichat.ui.company;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.ydd.zhichat.AppConstant;
import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.company.Department;
import com.ydd.zhichat.bean.company.StructBean;
import com.ydd.zhichat.bean.company.StructBeanNetInfo;
import com.ydd.zhichat.db.InternationalizationHelper;
import com.ydd.zhichat.helper.AvatarHelper;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.ui.other.BasicInfoActivity;
import com.ydd.zhichat.util.ScreenUtil;
import com.ydd.zhichat.util.SkinUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.view.MarqueeTextView;
import com.ydd.zhichat.view.SelectCpyPopupWindow;
import com.ydd.zhichat.view.TipDialog;
import com.ydd.zhichat.view.VerifyDialog;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import fm.jiecao.jcvideoplayer_lib.MessageEvent;
import okhttp3.Call;

/**
 * ????????????
 */
public class ManagerCompany extends BaseActivity {
    private static Context mContext;
    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    private List<StructBeanNetInfo> mStructData;// ??????????????????????????????
    private List<StructBean> mStructCloneData;
    private List<Department> mDepartments;
    private List<String> userList;
    private List<String> forCurrentSonDepart;
    private List<String> forCurrenttwoSonDepart;
    private List<String> forCurrentthrSonDepart;
    private SelectCpyPopupWindow mSelectCpyPopupWindow;
    private String mLoginUserId;
    // ??????????????????????????????
    private View.OnClickListener itemsOnClick = new View.OnClickListener() {

        public void onClick(View v) {
            mSelectCpyPopupWindow.dismiss();
            switch (v.getId()) {
                case R.id.btn_add_son_company:
                    startActivity(new Intent(ManagerCompany.this, CreateCompany.class));
                    break;
            }
        }
    };
    private String mCompanyCreater;// ???????????????
    private String mCompanyId;     // ??????id
    private String rootDepartment;

    public static void start(Context ctx) {
        mContext = ctx;
        Intent intent = new Intent(ctx, ManagerCompany.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_company);
        mLoginUserId = coreManager.getSelf().getUserId();
        initActionBar();
        initView();
        initData();
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("MY_COLLEAGUES"));
        ImageView ivRight = (ImageView) findViewById(R.id.iv_title_right);
        ivRight.setImageResource(R.drawable.ic_app_add);
        ivRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSelectCpyPopupWindow = new SelectCpyPopupWindow(ManagerCompany.this, itemsOnClick);
                // ????????????????????????????????????????????????????????????
                mSelectCpyPopupWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                // +x???,-x???,+y???,-y???
                // pop??????????????????
                mSelectCpyPopupWindow.showAsDropDown(view, -(mSelectCpyPopupWindow.getContentView().getMeasuredWidth() - view.getWidth() / 2 - 25), 0);
                darkenBackground(0.6f);
                mSelectCpyPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        darkenBackground(1.0f);
                    }
                });
            }
        });
    }

    private void initView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.companyRecycle);
        mAdapter = new MyAdapter(this);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mAdapter);

        EventBus.getDefault().register(this);
    }

    private void initData() {
        mStructData = new ArrayList<>();
        mStructCloneData = new ArrayList<>();

        mDepartments = new ArrayList<>();
        userList = new ArrayList<>();
        forCurrentSonDepart = new ArrayList<>();
        forCurrenttwoSonDepart = new ArrayList<>();
        forCurrentthrSonDepart = new ArrayList<>();
        loadData();
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEvent message) {
        if (message.message.equals("Update")) {// ??????
            initData();
        }
    }

    private void loadData() {
        // ??????userId??????????????????
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", coreManager.getSelf().getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().AUTOMATIC_SEARCH_COMPANY)
                .params(params)
                .build()
                .execute(new ListCallback<StructBeanNetInfo>(StructBeanNetInfo.class) {
                    @Override
                    public void onResponse(ArrayResult<StructBeanNetInfo> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            // ?????????????????????
                            mStructData = result.getData();
                            if (mStructData == null || mStructData.size() == 0) {
                                // ?????????null
                                Toast.makeText(ManagerCompany.this, R.string.tip_no_data, Toast.LENGTH_SHORT).show();
                            } else {
                                // ????????????
                                setData(mStructData);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(ManagerCompany.this, R.string.check_network, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setData(List<StructBeanNetInfo> data) {
        StructBean structBean;
        for (int i = 0; i < data.size(); i++) {
            structBean = new StructBean();
            // ?????????????????????pId?????????1????????????
            structBean.setParent_id("1");
            // ??????id
            mCompanyId = data.get(i).getId();
            structBean.setId(mCompanyId);
            mCompanyCreater = String.valueOf(data.get(i).getCreateUserId());
            structBean.setCreateUserId(mCompanyCreater);
            structBean.setCompanyId(mCompanyId);
            structBean.setRootDepartmentId(data.get(i).getRootDpartId().get(0));
            // ????????????
            structBean.setText(data.get(i).getCompanyName());
            // ????????????
            if (TextUtils.isEmpty(data.get(i).getNoticeContent())) {
                structBean.setNotificationDes(getString(R.string.no_notice));
            } else {
                structBean.setNotificationDes(data.get(i).getNoticeContent());
            }
            // ?????????id
            rootDepartment = data.get(i).getRootDpartId().get(0);
            /*
            ????????????
             */
            // ????????????
            structBean.setExpand(false);
            // ??????
            structBean.setIndex(0);
            structBean.setCompany(true);
            structBean.setDepartment(false);
            structBean.setEmployee(false);
            //
            mStructCloneData.add(structBean);
            /**
             * ??????????????????????????????
             */
            List<StructBeanNetInfo.DepartmentsBean> dps = data.get(i).getDepartments();
            for (int j = 0; j < dps.size(); j++) {
                // ????????????
                Department department = new Department();
                department.setDepartmentId(dps.get(j).getId());
                department.setDepartmentName(dps.get(j).getDepartName());
                department.setBelongToCompany(dps.get(j).getCompanyId());
                mDepartments.add(department);

                structBean = new StructBean();
                // ??????????????????
                int employeeIndex = 2;
                // ????????????????????????????????????
                boolean otherSon = false;
                // ??????:?????????????????????parentid???????????????id,??????????????????
                if (!dps.get(j).getId().equals(rootDepartment)) {
                    structBean.setParent_id(dps.get(j).getCompanyId());
                }
                // ????????????parentid????????????id,?????????????????????parentid?????????id
                if (rootDepartment.equals(dps.get(j).getParentId()) || mCompanyId.equals(dps.get(j).getParentId())) {
                    // ????????????
                    forCurrentSonDepart.add(dps.get(j).getId());
                    structBean.setIndex(1);
                    // ???????????????????????????2
                    employeeIndex = 2;
                    otherSon = true;
                }
                /*
                ?????????????????????????????????????????????
                 */
                for (int k = 0; k < forCurrentSonDepart.size(); k++) {
                    // ??????????????????,?????????????????????parentId?????????????????????????????????Id?????????????????????????????????
                    if (forCurrentSonDepart.get(k).equals(dps.get(j).getParentId())) {
                        // ???????????????????????????
                        forCurrenttwoSonDepart.add(dps.get(j).getId());
                        // ????????????????????????parent_id
                        structBean.setParent_id(dps.get(j).getParentId());
                        // ????????????
                        structBean.setIndex(2);
                        employeeIndex = 3;
                        otherSon = true;
                    }
                }
                for (int k = 0; k < forCurrenttwoSonDepart.size(); k++) {
                    // ??????????????????,?????????????????????parentId?????????????????????????????????Id?????????????????????????????????
                    if (forCurrenttwoSonDepart.get(k).equals(dps.get(j).getParentId())) {
                        forCurrentthrSonDepart.add(dps.get(j).getId());
                        // ????????????????????????parent_id
                        structBean.setParent_id(dps.get(j).getParentId());
                        // ????????????
                        structBean.setIndex(3);
                        employeeIndex = 4;
                        otherSon = true;
                    }
                }
                for (int k = 0; k < forCurrentthrSonDepart.size(); k++) {
                    // ??????????????????,?????????????????????parentId?????????????????????????????????Id?????????????????????????????????
                    if (forCurrentthrSonDepart.get(k).equals(dps.get(j).getParentId())) {
                        // ????????????????????????parent_id
                        structBean.setParent_id(dps.get(j).getParentId());
                        // ????????????
                        structBean.setIndex(4);
                        employeeIndex = 5;
                        otherSon = true;
                    }
                }
                if (!otherSon) {
                    // ???????????????????????????????????????????????????????????????????????????5???
                    // ????????????????????????parent_id
                    structBean.setParent_id(dps.get(j).getParentId());
                    // ????????????
                    structBean.setIndex(5);
                    employeeIndex = 6;
                }
                // ??????id
                structBean.setId(dps.get(j).getId());
                // ??????id
                structBean.setCompanyId(dps.get(j).getCompanyId());

                // ???????????????id?????????????????????????????????
                structBean.setCreateUserId(mCompanyCreater);
                // ????????????
                structBean.setText(dps.get(j).getDepartName());
                // ?????????????????????userId,???????????????????????????????????????
                List<StructBeanNetInfo.DepartmentsBean.EmployeesBean> empList = dps.get(j).getEmployees();

                for (StructBeanNetInfo.DepartmentsBean.EmployeesBean employeesBean : empList) {
                    int userId = employeesBean.getUserId();
                    userList.add(String.valueOf(userId));
                }
                /*
                ????????????
                 */
                structBean.setExpand(false);
                /*
                ????????????
                 */
                structBean.setCompany(false);
                structBean.setDepartment(true);
                structBean.setEmployee(false);
                mStructCloneData.add(structBean);
                /**
                 * ??????????????????????????????
                 */
                List<StructBeanNetInfo.DepartmentsBean.EmployeesBean> eps = dps.get(j).getEmployees();
                for (int z = 0; z < eps.size(); z++) {
                    structBean = new StructBean();
                    // ??????:??????id
                    structBean.setParent_id(eps.get(z).getDepartmentId());
                    // ??????id
                    structBean.setId(eps.get(z).getId());
                    // ??????id
                    structBean.setDepartmentId(eps.get(z).getDepartmentId());
                    // ??????id
                    structBean.setCompanyId(eps.get(z).getCompanyId());

                    // ???????????????
                    structBean.setCreateUserId(mCompanyCreater);
                    structBean.setEmployeeToCompanyId(eps.get(z).getCompanyId());
                    // employee name/id/role
                    structBean.setText(eps.get(z).getNickname());
                    structBean.setUserId(String.valueOf(eps.get(z).getUserId()));
                    structBean.setIdentity(eps.get(z).getPosition());
                    structBean.setRole(eps.get(z).getRole());
                    // ???????????????
                    structBean.setRootDepartmentId(rootDepartment);
                    structBean.setExpand(false);
                    if (employeeIndex == 2) {
                        structBean.setIndex(2);
                    } else if (employeeIndex == 3) {
                        structBean.setIndex(3);
                    } else if (employeeIndex == 4) {
                        structBean.setIndex(4);
                    } else if (employeeIndex == 5) {
                        structBean.setIndex(5);
                    } else {
                        structBean.setIndex(6);
                    }
                    structBean.setCompany(false);
                    structBean.setDepartment(false);
                    structBean.setEmployee(true);
                    mStructCloneData.add(structBean);
                }
            }
        }
        mAdapter.setData(mStructCloneData);
    }

    // ????????????
    private void exitCompany(String companyId, String userId) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("companyId", companyId);
        params.put("userId", userId);

        HttpUtils.get().url(coreManager.getConfig().EXIT_COMPANY)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(ManagerCompany.this, R.string.exi_c_succ, Toast.LENGTH_SHORT).show();
                        } else {
                            // ??????????????????
                            Toast.makeText(ManagerCompany.this, R.string.exi_c_fail, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(ManagerCompany.this);
                    }
                });
    }

    // ????????????
    private void deleteCompany(String companyId, String userId) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("companyId", companyId);
        params.put("userId", userId);

        HttpUtils.get().url(coreManager.getConfig().DELETE_COMPANY)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(ManagerCompany.this, R.string.del_c_succ, Toast.LENGTH_SHORT).show();
                        } else {
                            // ??????????????????
                            Toast.makeText(ManagerCompany.this, R.string.del_c_fail, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(ManagerCompany.this);
                    }
                });
    }

    // ????????????
    private void deleteDepartment(String departmentId) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("departmentId", departmentId);

        HttpUtils.get().url(coreManager.getConfig().DELETE_DEPARTMENT)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(ManagerCompany.this, R.string.del_d_succ, Toast.LENGTH_SHORT).show();
                            for (int i = 0; i < mDepartments.size(); i++) {
                                if (mDepartments.get(i).getDepartmentId().equals(departmentId)) {
                                    mDepartments.remove(i);
                                }
                            }
                        } else {
                            // ??????????????????
                            Toast.makeText(ManagerCompany.this, R.string.del_d_fail, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(ManagerCompany.this);
                    }
                });
    }

    // ????????????
    private void deleteEmployee(String userId, String departmentId) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("departmentId", departmentId);
        params.put("userIds", userId);

        HttpUtils.get().url(coreManager.getConfig().DELETE_EMPLOYEE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(ManagerCompany.this, R.string.del_e_succ, Toast.LENGTH_SHORT).show();
                        } else {
                            // ??????????????????
                            Toast.makeText(ManagerCompany.this, R.string.del_e_fail, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(ManagerCompany.this);
                    }
                });
    }

    // ??????????????????
    private void changeNotification(String companyId, String notifiContent) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("companyId", companyId);
        params.put("noticeContent", notifiContent);

        HttpUtils.get().url(coreManager.getConfig().MODIFY_COMPANY_NAME)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(mContext, getString(R.string.modify_succ), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext, getString(R.string.modify_fail), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(ManagerCompany.this);
                    }
                });
    }

    // ??????????????????
    private void changeEmployeeIdentity(String companyId, String userId, String position) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("companyId", companyId);
        params.put("userId", userId);
        params.put("position", position);

        HttpUtils.get().url(coreManager.getConfig().CHANGE_EMPLOYEE_IDENTITY)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(mContext, getString(R.string.modify_succ), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext, getString(R.string.modify_fail), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(ManagerCompany.this);
                    }
                });
    }

    /**
     * ???????????????????????????????????????????????????
     */
    private boolean IdentityOpentior(String createUserId) {
        boolean flag;
        String userId = coreManager.getSelf().getUserId();
        if (userId.equals(createUserId)) {
            flag = true;
        } else {
            flag = false;
        }
        return flag;
    }

    /**
     * ?????????????????????
     */
    private void darkenBackground(Float alpha) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = alpha;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setAttributes(lp);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    interface ItemClickListener {
        void onItemClick(int layoutPosition);

        void onAddClick(int layoutPosition);

        void notificationClick(int layoutPosition);
    }

    class MyAdapter extends RecyclerView.Adapter<StructHolder> {
        // ??????????????????????????????
        List<StructBean> mData;
        // ?????????????????????
        List<StructBean> currData;
        LayoutInflater mInflater;
        Context mContext;
        ItemClickListener mListener;
        PopupWindow mPopupWindow;
        View view;

        public MyAdapter(Context context) {
            mData = new ArrayList<>();
            currData = new ArrayList<>();
            mInflater = LayoutInflater.from(context);
            this.mContext = context;
        }

        public void setOnItemClickListener(ItemClickListener listener) {
            mListener = listener;
        }

        public void setData(List<StructBean> data) {
            mData = data;
            currData.clear();
            for (int i = 0; i < mData.size(); i++) {
                StructBean info = mData.get(i);
                if (info.getParent_id() != null) {
                    if (info.getParent_id().equals("1")) {
                        // ??????????????????????????????????????????
                        currData.add(info);
                        if (i == 0) {
                            info.setExpand(true);
                            openItemData(info.getId(), 0);
                        }
                    }
                }
            }
            notifyDataSetChanged();
        }

        /**
         * ????????????(??????)????????????????????????
         */
        private void showView(boolean group, StructHolder holder) {
            if (group) {
                holder.rlGroup.setVisibility(View.VISIBLE);
                holder.rlPersonal.setVisibility(View.GONE);
            } else {
                holder.rlGroup.setVisibility(View.GONE);
                holder.rlPersonal.setVisibility(View.VISIBLE);
            }
        }

        /**
         * ??????item
         */
        private void openItemData(String id, int position) {
            for (int i = mData.size() - 1; i > -1; i--) {
                StructBean data = mData.get(i);
                // ??????parent_id???currData????????????
                if (id.equals(data.getParent_id())) {
                    data.setIndex(data.getIndex() + 1);
                    currData.add(position + 1, data);
                }
            }
            notifyDataSetChanged();
        }

        /**
         * ??????item
         */
        private void closeItemData(String id, int position) {
            StructBean structBean = currData.get(position);
            if (structBean.isCompany()) {
                for (int i = currData.size() - 1; i >= 0; i--) {
                    StructBean data = currData.get(i);
                    if (data.getId().equals(structBean.getId()) || data.getCompanyId().equals(structBean.getId())) { // ?????? || ??????????????????&??????
                        if (data.isCompany()) { // ??????
                            data.setExpand(false);
                        } else if (data.isDepartment()) { // ??????
                            data.setExpand(false);
                            data.setIndex(data.getIndex() - 1);
                            currData.remove(i);
                        } else if (data.isEmployee()) { // ??????
                            data.setIndex(data.getIndex() - 1);
                            currData.remove(i);
                        }
                    }
                }
            } else if (structBean.isDepartment()) {
                for (int i = currData.size() - 1; i >= 0; i--) {
                    StructBean data = currData.get(i);
                    if (data.getId().equals(structBean.getId()) || data.getParent_id().equals(structBean.getId())) {
                        if (data.getId().equals(structBean.getId())) {
                            data.setExpand(false);
                        } else {// ??????????????????????????????????????????????????????????????????remove???
                            if (data.isDepartment()) {
                                data.setExpand(false);
                            }
                            data.setIndex(data.getIndex() - 1);
                            currData.remove(i);
                        }
                    }
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public StructHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = mInflater.inflate(R.layout.manager_company_item, null);
            final View add = view.findViewById(R.id.iv_group_add);
            final View add2 = view.findViewById(R.id.iv_group_add2);
            StructHolder holder = new StructHolder(view, new ItemClickListener() {
                // item click
                @Override
                public void onItemClick(int layoutPosition) {
                    StructBean bean = currData.get(layoutPosition);
                    // company/department click
                    if (bean.isExpand()) {
                        bean.setExpand(false);
                        closeItemData(bean.getId(), layoutPosition);
                    } else {
                        bean.setExpand(true);
                        openItemData(bean.getId(), layoutPosition);
                    }
                    // employee click
                    if (bean.isEmployee()) {
                        showEmployeeInfo(add2, layoutPosition);
                    }
                }

                // add click show company/department Opt
                @Override
                public void onAddClick(int layoutPosition) {
                    showAddDialog(add, layoutPosition);
                }

                // notice click
                @Override
                public void notificationClick(int layoutPosition) {
                    showNotification(layoutPosition);
                }

            });
            return holder;
        }

        @Override
        public void onBindViewHolder(StructHolder holder, int position) {
            StructBean bean = currData.get(position);
            showView(bean.isCompany() || bean.isDepartment(), holder);
            if (bean.isCompany() || bean.isDepartment()) {
                if (bean.isExpand()) {
                    holder.ivGroup.setImageResource(R.mipmap.ex);
                    holder.ivGroupAdd.setVisibility(View.VISIBLE);
                } else {
                    holder.ivGroup.setImageResource(R.mipmap.ec);
                    holder.ivGroupAdd.setVisibility(View.VISIBLE);
                }
                if (bean.isCompany()) {
                    // ????????????
                    holder.tvNotificationDes.setText(bean.getNotificationDes());
                    holder.rlNotification.setVisibility(View.VISIBLE);
                    // ??????????????????
                    // holder.rlGroup.setBackgroundColor(getResources().getAccentColor(R.color.department_item));
                } else if (bean.isDepartment()) {
                    // ????????????
                    holder.rlNotification.setVisibility(View.GONE);
                    // holder.rlGroup.setBackgroundColor(getResources().getAccentColor(R.color.person_item));
                }
                holder.tvGroupText.setText(bean.getText());
                // ??????????????????padding
                holder.rlGroup.setPadding(22 * bean.getIndex(), 0, 0, 0);
            } else {
                // ??????
                AvatarHelper.getInstance().displayAvatar(bean.getText(), bean.getUserId(), holder.ivInco, true);
                holder.tvTextName.setText(bean.getText());
                holder.tvIdentity.setText(bean.getIdentity());
                holder.rlPersonal.setPadding(22 * bean.getIndex(), 0, 0, 0);
            }
        }

        @Override
        public int getItemCount() {
            return currData.size();
        }

        private void showAddDialog(View add, final int layoutPosition) {
            final StructBean bean = currData.get(layoutPosition);
            if (bean.isCompany()) {
                view = mInflater.inflate(R.layout.popu_company, null);
            }
            if (bean.isDepartment()) {
                view = mInflater.inflate(R.layout.popu_department, null);
            }

            mPopupWindow = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, true);

            mPopupWindow.setTouchable(true);
            mPopupWindow.setOutsideTouchable(true);
            mPopupWindow.setBackgroundDrawable(new BitmapDrawable(getResources(), (Bitmap) null));
            int windowPos[] = calculatePopWindowPos(add, view);
            int xOff = 25;
            windowPos[0] -= xOff;
            mPopupWindow.showAtLocation(view, Gravity.TOP | Gravity.START, windowPos[0], windowPos[1]);

            /**
             * ????????????
             */
            boolean flag = IdentityOpentior(bean.getCreateUserId());
            if (bean.isCompany()) {
                if (!flag) {// ????????????
                    // ?????????????????????????????????????????????????????????????????????
                    TextView tv1 = (TextView) view.findViewById(R.id.tv_add_department);
                    TextView tv2 = (TextView) view.findViewById(R.id.tv_motify_cpn);
                    TextView tv3 = (TextView) view.findViewById(R.id.tv_delete_company);
                    tv1.setEnabled(false);
                    tv2.setEnabled(false);
                    tv3.setEnabled(false);
                    tv1.setTextColor(getResources().getColor(R.color.color_text));
                    tv2.setTextColor(getResources().getColor(R.color.color_text));
                    tv3.setTextColor(getResources().getColor(R.color.color_text));
                }
            }
            if (bean.isDepartment()) {
                if (!flag) {// ????????????
                    TextView tv1 = (TextView) view.findViewById(R.id.tv_add_group);
                    TextView tv2 = (TextView) view.findViewById(R.id.tv_motify_dmn);
                    TextView tv3 = (TextView) view.findViewById(R.id.tv_delete_department);
                    tv1.setEnabled(false);
                    tv2.setEnabled(false);
                    tv3.setEnabled(false);
                    tv1.setTextColor(getResources().getColor(R.color.color_text));
                    tv2.setTextColor(getResources().getColor(R.color.color_text));
                    tv3.setTextColor(getResources().getColor(R.color.color_text));
                    // ???????????????????????????
                    TextView tv4 = (TextView) view.findViewById(R.id.tv_add_employee);
                    tv4.setEnabled(false);
                    tv4.setTextColor(getResources().getColor(R.color.color_text));
                }
            }
            darkenBackground(0.6f);
            mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    darkenBackground(1.0f);
                }
            });
            /**
             * ????????????
             */
            if (bean.isCompany()) {
                view.findViewById(R.id.tv_add_department).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // ????????????
                        Intent intent = new Intent(ManagerCompany.this, CreateDepartment.class);
                        intent.putExtra("companyId", bean.getId());
                        intent.putExtra("rootDepartmentId", bean.getRootDepartmentId());
                        startActivity(intent);
                        mPopupWindow.dismiss();
                        // finish();
                    }
                });

                view.findViewById(R.id.tv_delete_company).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // ????????????
                        mPopupWindow.dismiss();
                        TipDialog tipDialog = new TipDialog(mContext);
                        tipDialog.setmConfirmOnClickListener(getString(R.string.sure_delete_company), new TipDialog.ConfirmOnClickListener() {
                            @Override
                            public void confirm() {
                                String mId = coreManager.getSelf().getUserId();
                                if (mId.equals(bean.getCreateUserId())) {
                                    deleteCompany(bean.getId(), coreManager.getSelf().getUserId());
                                    initData();
                                    mAdapter.notifyDataSetChanged();
                                } else {
                                    Toast.makeText(ManagerCompany.this, getString(R.string.connot_del_company), Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                }
                            }
                        });
                        tipDialog.show();
                    }
                });

                view.findViewById(R.id.tv_motify_cpn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // ???????????????
                        Intent intent = new Intent(ManagerCompany.this, ModifyCompanyName.class);
                        intent.putExtra("companyId", bean.getId());
                        intent.putExtra("companyName", bean.getText());
                        startActivity(intent);
                        mPopupWindow.dismiss();
                        // finish();
                    }
                });

                view.findViewById(R.id.tv_quit_company).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPopupWindow.dismiss();
                        // ????????????
                        TipDialog tipDialog = new TipDialog(mContext);
                        tipDialog.setmConfirmOnClickListener(getString(R.string.sure_exit_company), new TipDialog.ConfirmOnClickListener() {
                            @Override
                            public void confirm() {
                                exitCompany(bean.getId(), coreManager.getSelf().getUserId());
                                initData();
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                        tipDialog.show();
                    }
                });
            }

            /**
             * ????????????
             */
            if (bean.isDepartment()) {
                view.findViewById(R.id.tv_add_employee).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // ????????????
                        Intent intent = new Intent(ManagerCompany.this, AddEmployee.class);
                        intent.putExtra("departmentId", bean.getId());
                        intent.putExtra("companyId", bean.getCompanyId());
                        intent.putExtra("userList", JSON.toJSONString(userList));
                        startActivity(intent);
                        mPopupWindow.dismiss();
                        // finish();
                    }
                });
                view.findViewById(R.id.tv_add_group).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // ????????????
                        Intent intent = new Intent(ManagerCompany.this, CreateGroup.class);
                        intent.putExtra("companyId", bean.getCompanyId());
                        intent.putExtra("parentId", bean.getId());
                        startActivity(intent);
                        mPopupWindow.dismiss();
                        // finish();
                    }
                });
                view.findViewById(R.id.tv_delete_department).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPopupWindow.dismiss();
                        // TODO do your want
                        // ????????????
                        int emp = 0;
                        for (int i = 0; i < mStructData.size(); i++) {
                            List<StructBeanNetInfo.DepartmentsBean> departmentsBeans = mStructData.get(i).getDepartments();
                            if (departmentsBeans != null) {
                                for (int i1 = 0; i1 < departmentsBeans.size(); i1++) {
                                    if (departmentsBeans.get(i1).getId().equals(bean.getId())) {
                                        emp = departmentsBeans.get(i1).getEmpNum();
                                    }
                                }
                            }
                        }
                        if (emp > 0) {
                            DialogHelper.tip(ManagerCompany.this, getString(R.string.have_person_connot_del));
                            return;
                        }
                        deleteDepartment(bean.getId());
                        currData.remove(layoutPosition);
                        for (int i = 0; i < mData.size(); i++) {
                            // ?????????
                            if (mData.get(i).getId().equals(bean.getId())) {
                                mData.remove(i);
                            }
                        }
                        mAdapter.notifyDataSetChanged();
                    }
                });
                view.findViewById(R.id.tv_motify_dmn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // ??????????????????
                        Intent intent = new Intent(ManagerCompany.this, ModifyDepartmentName.class);
                        intent.putExtra("departmentId", bean.getId());
                        intent.putExtra("departmentName", bean.getText());
                        startActivity(intent);
                        mPopupWindow.dismiss();
                    }
                });
            }
        }

        private void showEmployeeInfo(final View asView, final int layoutPosition) {
            final StructBean bean = currData.get(layoutPosition);
            View view = mInflater.inflate(R.layout.popu_employee, null);
            mPopupWindow = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, true);

            mPopupWindow.setTouchable(true);
            mPopupWindow.setOutsideTouchable(true);
            mPopupWindow.setBackgroundDrawable(new BitmapDrawable(getResources(), (Bitmap) null));
            int windowPos[] = calculatePopWindowPos(asView, view);
            int xOff = 25;
            windowPos[0] -= xOff;
            mPopupWindow.showAtLocation(view, Gravity.TOP | Gravity.START, windowPos[0], windowPos[1]);

            boolean flag = IdentityOpentior(bean.getCreateUserId());
            if (flag) {
                // ??????????????????????????????
            } else {
                // ??????'??????'
                TextView tv1 = (TextView) view.findViewById(R.id.tv_change_department);
                TextView tv2 = (TextView) view.findViewById(R.id.tv_delete_employee);
                tv1.setEnabled(false);
                tv2.setEnabled(false);
                tv1.setTextColor(getResources().getColor(R.color.color_text));
                tv2.setTextColor(getResources().getColor(R.color.color_text));
            }
            if (bean.getUserId().equals(coreManager.getSelf().getUserId())) {
                // ????????????????????????????????????

            } else {
                TextView tv = (TextView) view.findViewById(R.id.tv_modify_position);
                tv.setTextColor(getResources().getColor(R.color.color_text));
            }
            darkenBackground(0.6f);
            mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    darkenBackground(1.0f);
                }
            });

            /**
             * ????????????
             */
            view.findViewById(R.id.tv_basic_employee).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // ??????
                    String userId = bean.getUserId();
                    Intent intent = new Intent(getApplicationContext(), BasicInfoActivity.class);
                    intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
                    startActivity(intent);
                    mPopupWindow.dismiss();
                }
            });
            // TODO do your want
            view.findViewById(R.id.tv_delete_employee).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String userId = bean.getUserId();
                    String departmentId = bean.getDepartmentId();
                    String mLoginUser = coreManager.getSelf().getUserId();
                    if (userId.equals(bean.getCreateUserId())) {
                        Toast.makeText(ManagerCompany.this, R.string.create_connot_dels, Toast.LENGTH_SHORT).show();
                        mPopupWindow.dismiss();
                        return;
                    }
                    if (userId.equals(mLoginUser)) {
                        Toast.makeText(ManagerCompany.this, R.string.connot_del_self, Toast.LENGTH_SHORT).show();
                        mPopupWindow.dismiss();
                        return;
                    }
                    deleteEmployee(userId, departmentId);
                    // ????????????
                    currData.remove(layoutPosition);
                    for (int i = 0; i < mData.size(); i++) {
                        // ?????????
                        if (mData.get(i).getId().equals(bean.getId())) {
                            mData.remove(i);
                        }
                    }
                    for (int i = 0; i < mStructData.size(); i++) {
                        for (int i1 = 0; i1 < mStructData.get(i).getDepartments().size(); i1++) {
                            if (mStructData.get(i).getDepartments().get(i1).getId().equals(departmentId)) {
                                mStructData.get(i).getDepartments().get(i1).setEmpNum(mStructData.get(i).getDepartments().get(i1).getEmpNum() - 1);
                            }
                        }
                    }
                    mAdapter.notifyDataSetChanged();
                    mPopupWindow.dismiss();
                }
            });
            // TODO
            view.findViewById(R.id.tv_change_department).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // ????????????
                    List<String> mDepartmentIdList = new ArrayList<>();
                    List<String> mDepartmentNameList = new ArrayList<>();
                    for (int i = 0; i < mDepartments.size(); i++) {
                        // / ????????????????????????????????????
                        if (mDepartments.get(i).getBelongToCompany().equals(bean.getEmployeeToCompanyId()) && !mDepartments.get(i).getDepartmentId().equals(bean.getDepartmentId())) {
                            /// ??????????????????
                            if (!mDepartments.get(i).getDepartmentId().equals(bean.getRootDepartmentId())) {
                                // ?????????
                                mDepartmentIdList.add(mDepartments.get(i).getDepartmentId());
                                mDepartmentNameList.add(mDepartments.get(i).getDepartmentName());
                            }
                        }
                    }
                    Intent intent = new Intent(ManagerCompany.this, ChangeEmployeeDepartment.class);
                    intent.putExtra("companyId", bean.getEmployeeToCompanyId());
                    intent.putExtra("userId", bean.getUserId());
                    intent.putExtra("departmentIdList", JSON.toJSONString(mDepartmentIdList));
                    intent.putExtra("departmentNameList", JSON.toJSONString(mDepartmentNameList));
                    startActivity(intent);
                    mPopupWindow.dismiss();
                    // finish();
                }
            });
            view.findViewById(R.id.tv_modify_position).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPopupWindow.dismiss();
                    if (mLoginUserId.equals(bean.getUserId())) {
                        showIdentity(layoutPosition);
                    } else {
                        Toast.makeText(mContext, R.string.tip_change_job_self, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        /**
         * ????????????????????????y????????????anchorView?????????????????????????????????x???????????????????????????????????????
         * ??????anchorView?????????????????????????????????????????????????????????????????????
         * <p>
         * https://www.cnblogs.com/popfisher/p/5608436.html
         *
         * @param anchorView  ??????window???view
         * @param contentView window???????????????
         * @return window?????????????????????xOff, yOff??????
         */
        private int[] calculatePopWindowPos(final View anchorView, final View contentView) {
            final int windowPos[] = new int[2];
            final int anchorLoc[] = new int[2];
            // ????????????View????????????????????????????????????
            anchorView.getLocationOnScreen(anchorLoc);
            final int anchorHeight = anchorView.getHeight();
            // ?????????????????????
            final int screenHeight = ScreenUtil.getScreenHeight(anchorView.getContext());
            final int screenWidth = ScreenUtil.getScreenWidth(anchorView.getContext());
            contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            // ??????contentView?????????
            final int windowHeight = contentView.getMeasuredHeight();
            final int windowWidth = contentView.getMeasuredWidth();
            // ????????????????????????????????????????????????
            final boolean isNeedShowUp = (screenHeight - anchorLoc[1] - anchorHeight < windowHeight);
            if (isNeedShowUp) {
                windowPos[0] = screenWidth - windowWidth;
                windowPos[1] = anchorLoc[1] - windowHeight;
            } else {
                windowPos[0] = screenWidth - windowWidth;
                windowPos[1] = anchorLoc[1] + anchorHeight;
            }
            return windowPos;
        }

        private void showNotification(final int layoutPosition) {
            final StructBean bean = currData.get(layoutPosition);
            if (mLoginUserId.equals(bean.getCreateUserId())) {
                VerifyDialog verifyDialog = new VerifyDialog(ManagerCompany.this);
                verifyDialog.setVerifyClickListener(getString(R.string.public_news), new VerifyDialog.VerifyClickListener() {
                    @Override
                    public void cancel() {

                    }

                    @Override
                    public void send(String str) {
                        changeNotification(bean.getId(), str);

                        currData.get(layoutPosition).setNotificationDes(str);
                        notifyDataSetChanged();
                    }
                });
                verifyDialog.show();
            } else {
                Toast.makeText(ManagerCompany.this, R.string.tip_change_public_owner, Toast.LENGTH_SHORT).show();
            }
        }

        private void showIdentity(final int layoutPosition) {
            final StructBean bean = currData.get(layoutPosition);
            VerifyDialog verifyDialog = new VerifyDialog(ManagerCompany.this);
            verifyDialog.setVerifyClickListener(getString(R.string.change_job), new VerifyDialog.VerifyClickListener() {
                @Override
                public void cancel() {

                }

                @Override
                public void send(final String str) {
                    changeEmployeeIdentity(bean.getCompanyId(), bean.getUserId(), str);

                    currData.get(layoutPosition).setIdentity(str);
                    notifyDataSetChanged();
                }
            });
            verifyDialog.show();
        }
    }

    class StructHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // ??????/????????????
        TextView tvGroupText;
        // ????????????
        TextView tvTextName;
        // ????????????
        TextView tvIdentity;
        // ??????
        ImageView ivGroup;
        // ??????...
        ImageView ivGroupAdd;
        // ??????
        ImageView ivInco;
        // ????????????
        MarqueeTextView tvNotificationDes;
        // ??????/??????
        RelativeLayout rlGroup;
        // ??????
        LinearLayout rlNotification;
        // ??????
        LinearLayout rlPersonal;
        ItemClickListener mListener;

        public StructHolder(View itemView, ItemClickListener listener) {
            super(itemView);
            mListener = listener;
            tvGroupText = (TextView) itemView.findViewById(R.id.tv_group_name);
            tvTextName = (TextView) itemView.findViewById(R.id.tv_text_name);
            tvIdentity = (TextView) itemView.findViewById(R.id.tv_text_role);
            tvIdentity.setTextColor(SkinUtils.getSkin(mContext).getAccentColor());
            tvNotificationDes = itemView.findViewById(R.id.notification_des);
            tvNotificationDes.setTextColor(SkinUtils.getSkin(mContext).getAccentColor());
            ivGroup = (ImageView) itemView.findViewById(R.id.iv_arrow);
            ivGroupAdd = (ImageView) itemView.findViewById(R.id.iv_group_add);
            ivInco = (ImageView) itemView.findViewById(R.id.iv_inco);
            rlGroup = (RelativeLayout) itemView.findViewById(R.id.rl_group);
            rlNotification = (LinearLayout) itemView.findViewById(R.id.notification_ll);
            rlPersonal = (LinearLayout) itemView.findViewById(R.id.rl_personal);
            /**
             * ??????????????????
             */
            rlGroup.setOnClickListener(this);
            rlPersonal.setOnClickListener(this);
            ivGroupAdd.setOnClickListener(this);
            tvNotificationDes.setOnClickListener(this);
            tvIdentity.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.iv_group_add:
                    mListener.onAddClick(getLayoutPosition());
                    break;
                case R.id.notification_des:
                    mListener.notificationClick(getLayoutPosition());
                    break;
                default:
                    mListener.onItemClick(getLayoutPosition());
                    break;
            }
        }
    }
}
