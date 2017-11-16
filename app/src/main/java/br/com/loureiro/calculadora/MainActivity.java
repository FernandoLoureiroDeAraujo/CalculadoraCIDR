package br.com.loureiro.calculadora;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.loureiro.misc.SimpleLog;
import br.com.loureiro.misc.CalculateIP;

/**
 * Created by fernando on 08/10/17.
 */
public class MainActivity extends AppCompatActivity {

    /* CONSTANTES */
    private static final List<Integer> mPrefixoList = Arrays.asList(16,17,18,19,20,21,22,23,24,25,26,27,28,29,30);

    private static final String LAST_DIGIT_IP = "255";

    private static final int MIN_LENGTH = 0;
    private static final int MAX_LENGTH = 3;

    /* UI */
    private CardView cardView;

    private EditText mIP_1;
    private EditText mIP_2;
    private EditText mIP_3;
    private EditText mIP_4;

    private Spinner mPrefix;

    private Button mClean;
    private Button mCalculate;

    private TextView mAddress;
    private TextView mMask;
    private TextView mBroadcast;
    private TextView mRange;
    private TextView mAmountIPs;

    private ExpandableListView mExpandableListView;
    private ExpandableListAdapter mExpandableListAdapter;

    /* VARIAVEIS */
    private Map<String, List<String>> mMap;
    private List<String> mExpandableListTitle;

    private String IP;
    private String prefixo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            changeStatusBarColor();
            getViewObjects();

            editTextChangedListeners();

            adicionarPrefixo();
            calculateButton();
            cleanButton();
        } catch (Exception e) {
            SimpleLog.showMessage(getWindow().getDecorView(), "Erro inesperado");
            SimpleLog.error("ERROR", e);
        }
    }

    private void getViewObjects() {
        cardView = (CardView) findViewById(R.id.card_view_2);

        mIP_1 = (EditText) findViewById(R.id.vIP_1);
        mIP_2 = (EditText) findViewById(R.id.vIP_2);
        mIP_3 = (EditText) findViewById(R.id.vIP_3);
        mIP_4 = (EditText) findViewById(R.id.vIP_4);

        mIP_4.setTag("TAG-IP4");

        mPrefix = (Spinner) findViewById(R.id.vPrefixo);

        mCalculate = (Button) findViewById(R.id.vCalcularBotao);
        mClean = (Button) findViewById(R.id.vLimparBotao);

        mAddress = (TextView) findViewById(R.id.vEnderecoRedeValor);
        mMask = (TextView) findViewById(R.id.vMascaraRedeValor);
        mBroadcast = (TextView) findViewById(R.id.vEnderecoBroadcastValor);
        mRange = (TextView) findViewById(R.id.vAlcanceValor);
        mAmountIPs = (TextView) findViewById(R.id.vQuantidadeValor);

        mExpandableListView = (ExpandableListView) findViewById(R.id.expandableListView);
    }

    private void getIP() {
        StringBuilder str = new StringBuilder();
        str.append(mIP_1.getText()).append(".");
        str.append(mIP_2.getText()).append(".");
        str.append(mIP_3.getText()).append(".");
        str.append(mIP_4.getText());

        IP = str.toString();
    }

    private void adicionarPrefixo() {
        ArrayAdapter<Integer> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mPrefixoList);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mPrefix.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                prefixo = String.valueOf(adapterView.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        mPrefix.setAdapter(dataAdapter);
    }

    private void calculateButton() {
        mCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboardAndClearFocus();

                getIP();
                CalculateIP.Calculate calculate = calculateCIDR();

                if(calculate != null) {
                    addResult(calculate);
                }
            }
        });
    }

    private CalculateIP.Calculate calculateCIDR() {
        try {
            CalculateIP cidr = new CalculateIP(new StringBuilder().append(IP).append("/").append(prefixo).toString());
            CalculateIP.Calculate calculate = cidr.getInfo();
            return calculate;
        } catch (Exception e) {
            SimpleLog.showMessage(getWindow().getDecorView(), "IP inválido");
            return null;
        }
    }

    private Map<String, List<String>> listIPS(CalculateIP.Calculate calculate) {
        Map<String, List<String>> map = new LinkedHashMap<>();

        List<String> ips = new ArrayList<>();

        for (int i = 0; i < calculate.getAllAddresses().length; i++) {
            String currentIP = calculate.getAllAddresses()[i];

            ips.add(currentIP);

            if(cancelAsync) {
                map = new LinkedHashMap<>();
                break;
            }

            if(LAST_DIGIT_IP.equals(getLastDigitIP(calculate.getAllAddresses()[i]))) {
                setIpRange(currentIP, ips, map);
                ips = new ArrayList<>();
                continue;
            }

            if(i == calculate.getAllAddresses().length - 1) {
                setIpRange(currentIP, ips, map);
                ips = new ArrayList<>();
            }
        }

        return map;
    }

    private static void setIpRange(String currentIP, List<String> ips, Map<String, List<String>> map) {
        map.put(new StringBuilder().append(ips.get(0)).append(" - ").append(currentIP).toString(), ips);
    }

    private static String getLastDigitIP(String ip) {
        try {
            String[] s = ip.split("\\.");
            return s[3];
        }catch (Exception e) {
            return null;
        }
    }

    private void addResult(CalculateIP.Calculate calculate) {
        mAddress.setText(calculate.getNetworkAddress());
        mMask.setText(calculate.getNetmask());
        mBroadcast.setText(calculate.getBroadcastAddress());
        mRange.setText(new StringBuilder().append(calculate.getLowAddress()).append(" - ").append(calculate.getHighAddress()));
        mAmountIPs.setText(String.valueOf(calculate.getAddressCountLong()));

        mExpandableListView.setAdapter((BaseExpandableListAdapter)null);

        cardView.setVisibility(View.VISIBLE);

        ListIPsAsync async = new ListIPsAsync();
        async.execute(calculate);
    }

    private void cleanButton() {
        mClean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelAsync = true;

                cardView.setVisibility(View.INVISIBLE);

                mIP_1.setText(null);
                mIP_2.setText(null);
                mIP_3.setText(null);
                mIP_4.setText(null);

                hideKeyboardAndClearFocus();

                mPrefix.setSelection(0);

                mAddress.setText(null);
                mMask.setText(null);
                mBroadcast.setText(null);
                mRange.setText(null);
                mAmountIPs.setText(null);

                IP = null;
                prefixo = null;
            }
        });
    }

    private void hideKeyboardAndClearFocus() {
        mIP_1.requestFocus();

        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

        mIP_1.clearFocus();
    }

    private void editTextChangedListeners() {
        editTextChangedListener(mIP_1, mIP_1, mIP_2);
        editTextChangedListener(mIP_2, mIP_1, mIP_3);
        editTextChangedListener(mIP_3, mIP_2, mIP_4);
        editTextChangedListener(mIP_4, mIP_3, mIP_4);
    }

    private void editTextChangedListener(final  EditText currentEditText, final EditText fowardEditText, final EditText nextEditText) {
        currentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int count) {
                if(currentEditText.getText().toString().length() == MIN_LENGTH) {
                    fowardEditText.requestFocus();
                    mCalculate.setEnabled(false);
                    return;
                }

                if(currentEditText.getText().toString().length() == MAX_LENGTH) {
                    nextEditText.requestFocus();
                    return;
                }

                if("TAG-IP4".equals(currentEditText.getTag())) {
                    if(currentEditText.getText().toString().length() > MIN_LENGTH) {
                        mCalculate.setEnabled(true);
                    }
                    return;
                }
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void changeStatusBarColor() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.background));
    }

    private boolean cancelAsync;

    private class ListIPsAsync extends AsyncTask<CalculateIP.Calculate, Void, Map<String, List<String>>> {
        @Override
        protected Map<String, List<String>> doInBackground(CalculateIP.Calculate... params) {
            mMap = listIPS(params[0]);
            mExpandableListTitle = new ArrayList<>(mMap.keySet());
            return mMap;
        }

        @Override
        protected void onPostExecute(Map<String, List<String>> result) {
            mExpandableListAdapter = new MainActivityExpandableListAdapter(MainActivity.this, mExpandableListTitle, mMap);
            mExpandableListView.setAdapter(mExpandableListAdapter);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}
