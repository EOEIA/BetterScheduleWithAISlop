package cz.vitskalicky.lepsirozvrh.schoolsDatabase;


import android.os.Bundle;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jaredrummler.cyanea.app.CyaneaFragment;

import cz.vitskalicky.lepsirozvrh.R;
import cz.vitskalicky.lepsirozvrh.model.StatusInfo;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class SchoolsListFragment extends CyaneaFragment {
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    SchoolsAdapter adapter = null;
    SchoolsListViewModel viewModel = null;

    ProgressBar progressBar;
    TextView twInfo;
    EditText etSearch;
    TextView twError;
    ImageView ivError;

    private Function1<SchoolInfo, Unit> onItemClick = schoolInfo -> {return Unit.INSTANCE;};

    public SchoolsListFragment() {
        // Required empty public constructor
    }

    public void setOnItemClickListener(Function1<SchoolInfo, Unit> onItemClick){
        this.onItemClick = onItemClick;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_schools_list, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        twInfo = view.findViewById(R.id.textViewInfo);
        etSearch = view.findViewById(R.id.editTextSearch);
        twError = view.findViewById(R.id.textViewError);
        ivError = view.findViewById(R.id.imageViewError);

        viewModel = ViewModelProviders.of(this).get(SchoolsListViewModel.class);

        etSearch.addTextChangedListener(new TextWatcher() {//<editor-fold desc="unused methods">
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
            //</editor-fold>
            @Override
            public void afterTextChanged(Editable s) {
                if (viewModel != null){
                    viewModel.setQuery(s.toString());
                }
            }
        });

        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new SchoolsAdapter(getContext(), onItemClick);
        recyclerView.setAdapter(adapter);

        viewModel.getQueriedSchools().observe(getViewLifecycleOwner(), schoolInfos -> {
            adapter.submitList(schoolInfos);
        });


        viewModel.getStatusLD().observe(getViewLifecycleOwner(), statusInfo -> {
            if (statusInfo.getStatus() == StatusInfo.Status.SUCCESS){
                progressBar.setVisibility(View.GONE);
                twInfo.setVisibility(View.GONE);
                twError.setVisibility(View.GONE);
                ivError.setVisibility(View.GONE);
            } else if (statusInfo.getStatus() == StatusInfo.Status.LOADING || statusInfo.getStatus() == StatusInfo.Status.UNKNOWN){
                progressBar.setVisibility(View.VISIBLE);
                twInfo.setVisibility(View.GONE);
                twError.setVisibility(View.GONE);
                ivError.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.GONE);
                twInfo.setVisibility(View.GONE);
                twError.setVisibility(View.VISIBLE);
                ivError.setVisibility(View.VISIBLE);
            }
        });

        /*requestQueue = SchoolsDatabaseAPI.getAllSchools(getContext(), successful -> {
            if (successful) {



                //viewModel.setQuery(etSearch.getText().toString());
            }else {
                progressBar.setVisibility(View.GONE);
                twInfo.setVisibility(View.GONE);
                twError.setVisibility(View.VISIBLE);
                ivError.setVisibility(View.VISIBLE);
            }
        },database, progressBar);*/

        //automatically show keyboard
        etSearch.requestFocus();
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /*public static interface OnItemClickListener{
        public void onClick(String url);
    }*/
}
