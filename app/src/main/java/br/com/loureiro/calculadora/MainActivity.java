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

import org.apache.commons.net.util.SubnetUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.loureiro.misc.SimpleLog;

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

    private Spinner mPrefixo;

    private Button mLimpar;
    private Button mCalcular;

    private TextView mEndereco;
    private TextView mMascara;
    private TextView mBroadcast;
    private TextView mAlcance;
    private TextView mQuantidadeIPs;

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

            alterarCorStatusBar();
            getViewObjects();

            editTextChangedListeners();

            adicionarPrefixo();
            botaoCalcular();
            botaoLimpar();
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

        mPrefixo = (Spinner) findViewById(R.id.vPrefixo);

        mCalcular = (Button) findViewById(R.id.vCalcularBotao);
        mLimpar = (Button) findViewById(R.id.vLimparBotao);

        mEndereco = (TextView) findViewById(R.id.vEnderecoRedeValor);
        mMascara = (TextView) findViewById(R.id.vMascaraRedeValor);
        mBroadcast = (TextView) findViewById(R.id.vEnderecoBroadcastValor);
        mAlcance = (TextView) findViewById(R.id.vAlcanceValor);
        mQuantidadeIPs = (TextView) findViewById(R.id.vQuantidadeValor);

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

        mPrefixo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                prefixo = String.valueOf(adapterView.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        mPrefixo.setAdapter(dataAdapter);
    }

    private void botaoCalcular() {
        mCalcular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboardAndClearFocus();

                getIP();
                SubnetUtils.SubnetInfo subnetInfo = calcularCIDR();

                if(subnetInfo != null) {
                    adicionarResultado(subnetInfo);
                }
            }
        });
    }

    private SubnetUtils.SubnetInfo calcularCIDR() {
        try {
            SubnetUtils cidr = new SubnetUtils(new StringBuilder().append(IP).append("/").append(prefixo).toString());
            SubnetUtils.SubnetInfo subnetInfo = cidr.getInfo();
            return subnetInfo;
        } catch (Exception e) {
            SimpleLog.showMessage(getWindow().getDecorView(), "IP inválido");
            return null;
        }
    }

    private Map<String, List<String>> listarIPsCalculados(SubnetUtils.SubnetInfo subnetInfo) {
        Map<String, List<String>> map = new LinkedHashMap<>();

        List<String> ips = new ArrayList<>();

        for (int i = 0; i < subnetInfo.getAllAddresses().length; i++) {
            String currentIP = subnetInfo.getAllAddresses()[i];

            ips.add(currentIP);

            if(cancelAsync) {
                map = new LinkedHashMap<>();
                break;
            }

            if(LAST_DIGIT_IP.equals(getLastDigitIP(subnetInfo.getAllAddresses()[i]))) {
                setIpRange(currentIP, ips, map);
                ips = new ArrayList<>();
                continue;
            }

            if(i == subnetInfo.getAllAddresses().length - 1) {
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

    private void adicionarResultado(SubnetUtils.SubnetInfo subnetInfo) {
        mEndereco.setText(subnetInfo.getNetworkAddress());
        mMascara.setText(subnetInfo.getNetmask());
        mBroadcast.setText(subnetInfo.getBroadcastAddress());
        mAlcance.setText(new StringBuilder().append(subnetInfo.getLowAddress()).append(" - ").append(subnetInfo.getHighAddress()));
        mQuantidadeIPs.setText(String.valueOf(subnetInfo.getAddressCountLong()));

        mExpandableListView.setAdapter((BaseExpandableListAdapter)null);

        cardView.setVisibility(View.VISIBLE);

        ListarIPsCalculadosAsync async = new ListarIPsCalculadosAsync();
        async.execute(subnetInfo);
    }

    private void botaoLimpar() {
        mLimpar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelAsync = true;

                cardView.setVisibility(View.INVISIBLE);

                mIP_1.setText(null);
                mIP_2.setText(null);
                mIP_3.setText(null);
                mIP_4.setText(null);

                hideKeyboardAndClearFocus();

                mPrefixo.setSelection(0);

                mEndereco.setText(null);
                mMascara.setText(null);
                mBroadcast.setText(null);
                mAlcance.setText(null);
                mQuantidadeIPs.setText(null);

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
                    mCalcular.setEnabled(false);
                    return;
                }

                if(currentEditText.getText().toString().length() == MAX_LENGTH) {
                    nextEditText.requestFocus();
                    return;
                }

                if("TAG-IP4".equals(currentEditText.getTag())) {
                    if(currentEditText.getText().toString().length() > MIN_LENGTH) {
                        mCalcular.setEnabled(true);
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
    public void alterarCorStatusBar() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.background));
    }

    private boolean cancelAsync;

    private class ListarIPsCalculadosAsync extends AsyncTask<SubnetUtils.SubnetInfo, Void, Map<String, List<String>>> {
        @Override
        protected Map<String, List<String>> doInBackground(SubnetUtils.SubnetInfo... params) {
            mMap = listarIPsCalculados(params[0]);
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
