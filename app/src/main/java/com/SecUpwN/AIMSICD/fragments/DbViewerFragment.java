package com.SecUpwN.AIMSICD.fragments;

import com.SecUpwN.AIMSICD.R;
import com.SecUpwN.AIMSICD.adapters.AIMSICDDbAdapter;
import com.SecUpwN.AIMSICD.adapters.BaseInflaterAdapter;
import com.SecUpwN.AIMSICD.adapters.CardItemData;
import com.SecUpwN.AIMSICD.adapters.CellCardInflater;
import com.SecUpwN.AIMSICD.adapters.DefaultLocationCardInflater;
import com.SecUpwN.AIMSICD.adapters.OpenCellIdCardInflater;
import com.SecUpwN.AIMSICD.adapters.SilentSmsCardData;
import com.SecUpwN.AIMSICD.adapters.SilentSmsCardInflater;
import com.SecUpwN.AIMSICD.utils.Helpers;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

public class DbViewerFragment extends Fragment {

    private AIMSICDDbAdapter mDb;
    private String mTableSelected;
    private boolean mMadeSelection;
    private Context mContext;

    //Layout items
    private Spinner tblSpinner;
    private ListView lv;
    private View emptyView;

    public DbViewerFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity.getBaseContext();
        mDb = new AIMSICDDbAdapter(mContext);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.db_view,
                container, false);

        if (view != null) {
            lv = (ListView) view.findViewById(R.id.list_view);
            emptyView = view.findViewById(R.id.db_list_empty);

            tblSpinner = (Spinner) view.findViewById(R.id.table_spinner);
            tblSpinner.setOnItemSelectedListener(new spinnerListener());

            Button loadTable = (Button) view.findViewById(R.id.load_table_data);

            loadTable.setOnClickListener(new btnClick());
        }

        return view;
    }

    private class spinnerListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                int position, long id) {
            mTableSelected = String.valueOf(tblSpinner.getSelectedItem());
            mMadeSelection = true;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parentView) {
            mMadeSelection = false;
        }
    }

    private class btnClick implements View.OnClickListener {

        @Override
        public void onClick(final View v) {
            if (mMadeSelection) {
                v.setEnabled(false);
                getActivity().setProgressBarIndeterminateVisibility(true);
                lv.setVisibility(View.GONE);

                new AsyncTask<Void, Void, BaseInflaterAdapter> () {

                    @Override
                    protected BaseInflaterAdapter doInBackground(Void... params) {
                        mDb.open();
                        Cursor result = null;

                        switch (mTableSelected) {
                            case "Cell Data":
                                result = mDb.getCellData();
                                break;
                            case "Location Data":
                                result =  mDb.getLocationData();
                                break;
                            case "OpenCellID Data":
                                result =  mDb.getOpenCellIDData();
                                break;
                            case "Default MCC Locations":
                                result =  mDb.getDefaultMccLocationData();
                                break;
                            case "Silent Sms":
                                result =  mDb.getSilentSmsData();
                                break;
                        }

                        BaseInflaterAdapter adapter = null;
                        if (result != null) {
                            adapter = BuildTable(result);
                        }
                        mDb.close();

                        return adapter;
                    }

                    @Override
                    protected void onPostExecute(BaseInflaterAdapter adapter) {
                        if (getActivity() == null) return; // fragment detached

                        lv.setEmptyView(emptyView);
                        if (adapter != null) {
                            lv.setAdapter(adapter);
                            lv.setVisibility(View.VISIBLE);
                        } else {
                            lv.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);
                            //Helpers.msgShort(mContext, "Table contains no data to display.");
                        }

                        v.setEnabled(true);
                        getActivity().setProgressBarIndeterminateVisibility(false);
                    }
                }.execute();
            }
        }
    }

    private BaseInflaterAdapter BuildTable(Cursor tableData) {
        if (tableData != null && tableData.getCount() > 0) {
            switch (mTableSelected) {
                case "OpenCellID Data": {
                    BaseInflaterAdapter<CardItemData> adapter
                            = new BaseInflaterAdapter<>(
                            new OpenCellIdCardInflater());
                    int count = tableData.getCount();
                    while (tableData.moveToNext()) {
                        CardItemData data = new CardItemData("CellID: " + tableData.getString(0),
                                "LAC: " + tableData.getString(1), "MCC: " + tableData.getString(2),
                                "MNC: " + tableData.getString(3),
                                "Latitude: " + tableData.getString(4),
                                "Longitude: " + tableData.getString(5),
                                "Average Signal Strength: " + tableData.getString(6),
                                "Samples: " + tableData.getString(7),
                                "" + (tableData.getPosition() + 1) + " / " + count);
                        adapter.addItem(data, false);
                    }
                    return adapter;
                }
                case "Default MCC Locations": {
                    BaseInflaterAdapter<CardItemData> adapter
                            = new BaseInflaterAdapter<>(
                            new DefaultLocationCardInflater());
                    int count = tableData.getCount();
                    Log.d("mcc", "Got records " + count);
                    while (tableData.moveToNext()) {
                        CardItemData data = new CardItemData("Country: " + tableData.getString(0),
                                "MCC: " + tableData.getString(1),
                                "Latitude: " + tableData.getString(2),
                                "Longitude: " + tableData.getString(3),
                                "" + (tableData.getPosition() + 1) + " / " + count);
                        adapter.addItem(data, false);
                    }
                    Log.d("mcc", "Adapter has " + adapter.getCount());
                    return adapter;
                }
                case "Silent Sms": {
                    BaseInflaterAdapter<SilentSmsCardData> adapter
                            = new BaseInflaterAdapter<>(
                            new SilentSmsCardInflater());
                    while (tableData.moveToNext()) {
                        SilentSmsCardData data = new SilentSmsCardData(tableData.getString(0),
                                tableData.getString(1), tableData.getString(2),
                                tableData.getString(3),
                                tableData.getString(4), tableData.getLong(5));
                        adapter.addItem(data, false);
                    }
                    return adapter;
                }
                default: {
                    BaseInflaterAdapter<CardItemData> adapter
                            = new BaseInflaterAdapter<>(
                            new CellCardInflater());
                    int count = tableData.getCount();
                    while (tableData.moveToNext()) {
                        CardItemData data = new CardItemData("CellID: " + tableData.getString(0),
                                "LAC: " + tableData.getString(1),
                                "Network Type: " + tableData.getString(2),
                                "Latitude: " + tableData.getString(3),
                                "Longitude: " + tableData.getString(4),
                                "Signal Strength: " + tableData.getString(5),
                                "" + (tableData.getPosition() + 1) + " / " + count);
                        adapter.addItem(data, false);
                    }
                    return adapter;
                }
            }
        } else {
            return null;
        }
    }
}
