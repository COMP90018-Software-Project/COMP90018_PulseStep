package com.example.pulsestepapplication;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RankingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RankingFragment extends Fragment {

    ArrayList<RankModel> rankModels = new ArrayList<>();
    int[] rankUserImages = {R.drawable.sample_profile_img,
            R.drawable.sample_profile_img,
            R.drawable.sample_profile_img,
            R.drawable.sample_profile_img,
            R.drawable.sample_profile_img,
            R.drawable.sample_profile_img,
            R.drawable.sample_profile_img,
            R.drawable.sample_profile_img,
            R.drawable.sample_profile_img};

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public RankingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RankingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RankingFragment newInstance(String param1, String param2) {
        RankingFragment fragment = new RankingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        setUpRankModels();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_ranking, container, false);

        RecyclerView rankRecyclerView = view.findViewById(R.id.ranking_List);
        rankRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        RankListAdapter rankListAdapter = new RankListAdapter(requireContext(), rankModels);
        rankRecyclerView.setAdapter(rankListAdapter);

        return view;
    }

    private void setUpRankModels(){
        String[] rankNo = getResources().getStringArray(R.array.sample_ranking_no);
        String[] rankUserNames = getResources().getStringArray(R.array.sample_ranking_user_names);
        String[] rankWorkoutTimes = getResources().getStringArray(R.array.sample_ranking_times);

        for (int i = 0; i<rankNo.length; i++){
            rankModels.add(new RankModel(rankNo[i],
                    rankUserNames[i],
                    rankWorkoutTimes[i],
                    rankUserImages[i]));
        }

    }
}