package com.example.mobilfinal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class nav_uyeEkle_Fragment extends Fragment {
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;

    RecyclerView grupRV, rehberRV;
    TextView secilenGrupTV, uyekEkleTV;

    grupModel secilenGrup;

    ArrayList<grupModel> grupModels;
    ArrayList<rehberModel> rehberModelArrayList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_nav_uye_ekle_, container, false);
        grupRV = root.findViewById(R.id.uyeEkle_grupRV);
        rehberRV = root.findViewById(R.id.uyeekle_rehberRV);
        uyekEkleTV = root.findViewById(R.id.uyeEkle_secilenTxt);
        secilenGrupTV = root.findViewById(R.id.uyeEkle_secilenTxt);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        grupModels = new ArrayList<>();
        rehberModelArrayList = new ArrayList<>();
        ActivityResultLauncher launcher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (result) {
                rehberG??ster();
            } else {
                Toast.makeText(getContext(), "ki??ilere eri??im izni gerekiyor", Toast.LENGTH_SHORT).show();
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getContext().checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(Manifest.permission.READ_CONTACTS);
        } else {
            rehberG??ster();
        }
        grupG??ster();
        return root;

    }

    private void grupG??ster() {
        String userID = firebaseAuth.getCurrentUser().getUid();
        firebaseFirestore.collection("/userdata/" + userID + "/groups").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                grupModel grupModel2 = new grupModel(documentSnapshot.getString("name"),
                        documentSnapshot.getString("description"),
                        documentSnapshot.getString("image"),
                        documentSnapshot.getString("kullanici"),
                        documentSnapshot.getString("grupID"),
                        (List<String>) documentSnapshot.get("number"));
                grupModels.add(grupModel2);
            }
            grupRV.setAdapter(new uyeGrupAdapter(grupModels, p -> {
                secilenGrup = grupModels.get(p);
                secilenGrupTV.setText("Se??ti??iniz grup: " + secilenGrup.getGrupAdi());
            }));
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
            grupRV.setLayoutManager(linearLayoutManager);
        });
    }

    private void rehberG??ster() {
        Cursor cursor;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            cursor = getContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            rehberModelArrayList.clear();
            while(cursor.moveToNext()){
                @SuppressLint("Range") String ad=cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                @SuppressLint("Range") String numara=cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                @SuppressLint("Range") String resim=cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                rehberModel rehberModel2=new rehberModel(ad,numara,resim);
                rehberModelArrayList.add(rehberModel2);
            }
        }
        rehberRV.setAdapter(new rehberAdapter(rehberModelArrayList,p->{
            rehberModel rehberModel1=rehberModelArrayList.get(p);
            if(secilenGrup!=null){
                new AlertDialog.Builder(getContext()).setTitle("Ki??i Ekle").
                        setMessage(rehberModel1.getAd()+" ki??isini "+ secilenGrup.getGrupAdi()+" grubuna eklemek istiyor musunuz?").
                        setPositiveButton("Evet", (d,w)->{
                            firebaseFirestore.collection("/userdata/"+firebaseAuth.getCurrentUser().getUid()+"/groups").
                                    document(secilenGrup.getGrupId()).update(new HashMap<String,Object>(){{
                                        put("number", FieldValue.arrayUnion(rehberModel1.getNumara()));
                                    }}).addOnSuccessListener(aVoid ->{
                                        Toast.makeText(getContext(),"Ki??i eklendi",Toast.LENGTH_SHORT).show();
                                            });
                        }).setNegativeButton("Hay??r",null).show();
            }
        }));
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(getActivity(),LinearLayoutManager.VERTICAL,false);
        rehberRV.setLayoutManager(linearLayoutManager);
    }
}