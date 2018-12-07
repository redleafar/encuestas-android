package com.rafaelbermudez.encuestas.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.rafaelbermudez.encuestas.Entities.Poll;
import com.rafaelbermudez.encuestas.R;

import java.util.ArrayList;

public class PollsAdapter extends ArrayAdapter <Poll>{

    private int resource;
    private ArrayList<Poll> objects;
    private Context context;

    public PollsAdapter(@NonNull Context context, int resource, @NonNull ArrayList<Poll> objects) {

        super(context, resource, objects);
        this.resource = resource;
        this.objects = objects;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        TextView pollTitle;
        TextView nameAgeText;
        TextView travelingReasonText;
        TextView firstTimeTravelingText;
        TextView whatLikesMostText;
        ImageView icon;

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null){
            convertView = inflater.inflate(resource, null);
        }

        pollTitle = convertView.findViewById(R.id.title);
        nameAgeText = convertView.findViewById(R.id.name_and_age);
        travelingReasonText = convertView.findViewById(R.id.travel_reason);
        firstTimeTravelingText = convertView.findViewById(R.id.first_time_traveling);
        whatLikesMostText = convertView.findViewById(R.id.what_likes_most);
        icon = convertView.findViewById(R.id.icon);

        pollTitle.setText("Encuesta No. " + objects.get(position).getId());
        nameAgeText.setText(objects.get(position).getFirstname() + " " + objects.get(position).getLastname() + " de " + objects.get(position).getAge() + " años" );
        travelingReasonText.setText("Motivo de viaje: " + objects.get(position).getAnswer1());
        firstTimeTravelingText.setText("Viaja por primera vez: " + objects.get(position).getAnswer2());
        whatLikesMostText.setText("Le gusta de la región: " + objects.get(position).getAnswer3());

        if (objects.get(position).getUploaded() == 1){
            icon.setImageResource(R.drawable.uploaded);
        }
        else{
            icon.setImageResource(R.drawable.notuploaded);
        }

        return convertView;
    }
}
