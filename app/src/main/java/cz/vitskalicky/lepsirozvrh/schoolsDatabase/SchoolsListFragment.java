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

import com.jaredrummler.cyanea.app.CyaneaFragment;

import cz.vitskalicky.lepsirozvrh.R;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class SchoolsListFragment extends CyaneaFragment {
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    SchoolsAdapter adapter = null;
    SchoolsListViewModel viewModel = null;

    EditText etSearch;

    private Function2<SchoolInfo, Boolean, Unit> onItemClick = (schoolInfo, isManualUrl) -> {return Unit.INSTANCE;};

    public SchoolsListFragment() {
        // Required empty public constructor
    }

    public void setOnItemClickListener(Function2<SchoolInfo, Boolean, Unit> onItemClick){
        this.onItemClick = onItemClick;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_schools_list, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        etSearch = view.findViewById(R.id.editTextSearch);

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
                if (adapter != null){
                    adapter.setQueryText(s.toString());
                }
            }
        });

        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new SchoolsAdapter(requireContext(), (schoolInfo, isManualUrl) -> onItemClick.invoke(schoolInfo, isManualUrl), () -> {viewModel.refreshUnsuspend(); return Unit.INSTANCE;});
        adapter.setOnListChanged((previousList, currentList) -> {
            try {
                // this makes sure it is scrolled to top only if search query changes and not, for example, on screen rotation.
                if ((currentList != null && previousList != null && currentList.size() > 0) && (currentList.size() != previousList.size())) {
                    recyclerView.getLayoutManager().scrollToPosition(0);
                }
            }catch (NullPointerException ignored){};
            return Unit.INSTANCE;
        });
        recyclerView.setAdapter(adapter);

        viewModel.getQueriedSchools().observe(getViewLifecycleOwner(), schoolInfos -> {
            adapter.submitList(schoolInfos);
        });


        viewModel.getStatusLD().observe(getViewLifecycleOwner(), statusInfo -> {
            adapter.setStatus(statusInfo);
        });

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
