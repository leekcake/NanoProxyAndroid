package com.leekcake.kancolle.proxy.nanodesu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.leekcake.kancolle.proxy.nanodesu.container.Account;
import com.leekcake.kancolle.proxy.nanodesu.container.Fleet;
import com.leekcake.kancolle.proxy.nanodesu.receiver.AccountReceiver;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements AccountReceiver {
    public NanoProxyServiceDesu mService; // 연결 타입 서비스
    protected boolean mBound = false; // 서비스 연결 여부

    public boolean isFront = false;

    private ArrayList<Account> accounts = new ArrayList<>();
    private ArrayAdapter<Account> accounts_adapter;
    private Spinner accounts_spinner;

    private ArrayList<Fleet> fleets = new ArrayList<>();
    private FleetAdapter fleets_adapter;
    private ListView fleets_listview;

    private final class FleetUpdater extends Thread {
        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    Thread.sleep(1000);
                    updateFleet();
                }
            } catch (InterruptedException e) {
            }
        }
    }

    private final FleetUpdater updater = new FleetUpdater();

    protected ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            NanoProxyServiceDesu.LocalBinder binder = (NanoProxyServiceDesu.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            Log.v("MainActivity", "Connected!");
            mService.registerAccountReceiver(MainActivity.this);
            mService.resendAccountsToReceiver();
            updater.start();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main);

        getApplicationContext().startService(new Intent(getApplicationContext(), NanoProxyServiceDesu.class));
        bindService(new Intent(getApplicationContext(), NanoProxyServiceDesu.class), mConnection, Context.BIND_AUTO_CREATE);

        Button btn_stop = (Button) findViewById(R.id.btn_stop);
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert_confirm = new AlertDialog.Builder(MainActivity.this);
                alert_confirm.setMessage("서비스를 중단하고 어플을 닫겠습니까?").setCancelable(false).setPositiveButton("확인",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                stopService(new Intent(getApplicationContext(), NanoProxyServiceDesu.class));
                                finish();
                            }
                        }).setNegativeButton("취소",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                return;
                            }
                        });
                AlertDialog alert = alert_confirm.create();
                alert.show();
            }
        });

        Button btn_reset = (Button) findViewById(R.id.btn_reset);
        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert_confirm = new AlertDialog.Builder(MainActivity.this);
                alert_confirm.setMessage("현재 가지고 있는 모든 제독 정보를 제거합니까?").setCancelable(false).setPositiveButton("확인",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mService.clearAccounts();
                            }
                        }).setNegativeButton("취소",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                return;
                            }
                        });
                AlertDialog alert = alert_confirm.create();
                alert.show();
            }
        });

        accounts_spinner = (Spinner) findViewById(R.id.sp_account);
        accounts_adapter = new ArrayAdapter<Account>(this, android.R.layout.simple_spinner_dropdown_item, accounts);
        accounts_spinner.setAdapter(accounts_adapter);

        accounts_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateFleet();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        fleets_listview = (ListView) findViewById(R.id.lv_fleets);
        fleets_adapter = new FleetAdapter(this, fleets);
        fleets_listview.setAdapter(fleets_adapter);
    }

    @Override
    protected void onResume() {
        if(mBound) {
            mService.resendAccountsToReceiver();
        }
        super.onResume();
    }

    @Override
    public void onAccountDetected(final Account account) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                accounts.add(account);
                if(accounts.size() == 1) {
                    accounts_spinner.setSelection(0);
                    updateFleet();
                }
                accounts_adapter.notifyDataSetChanged();
            }
        });
    }

    public final void updateFleet() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(accounts.size() == 0) return;
                fleets.clear();
                fleets.addAll(((Account) accounts_spinner.getSelectedItem()).getFleets());
                fleets_adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onAccountChanged(final Account account) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Account nowSelected = ((Account) accounts_spinner.getSelectedItem());
                if( nowSelected != null && account.tokenID.equals( nowSelected.tokenID ) ) {
                    updateFleet();
                }
            }
        });
    }

    @Override
    public void onAccountCleared() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                accounts.clear();
                fleets.clear();
                accounts_adapter.notifyDataSetChanged();
                fleets_adapter.notifyDataSetChanged();
            }
        });
    }

    public final class FleetAdapter extends ArrayAdapter<Fleet> {
        protected final LayoutInflater inflater;

        public FleetAdapter(Context context, List<Fleet> fleetList) {
            super(context, R.layout.row_fleet, fleetList);
            this.inflater = LayoutInflater.from(context.getApplicationContext());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if(v == null) {
                v = inflater.inflate(R.layout.row_fleet, null);
            }
            Fleet fleet = getItem(position);
            TextView tv_name = (TextView) v.findViewById(R.id.tv_name);
            TextView tv_lefttime = (TextView) v.findViewById(R.id.tv_lefttime);

            tv_name.setText( "함대 명: " + fleet.name);
            tv_lefttime.setText("남은 시간: " + fleet.getLeftTime());

            return v;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unbindService(mConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
