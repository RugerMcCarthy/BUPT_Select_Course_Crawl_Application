package com.example.couseselector;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.zip.Inflater;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.ViewHolder> {

    private List<Course> courseList;
    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView courseName;
        TextView courseStatus;

        public ViewHolder(View view)
        {
            super(view);
            courseName = view.findViewById(R.id.course_name);
            courseStatus =  view.findViewById(R.id.course_status);
        }

    }

    public CourseAdapter(List<Course> courses) {
        this.courseList = courses;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.course_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Course course = courseList.get(position);
        holder.courseName.setText(course.courseName);
        holder.courseStatus.setText(course.selectStatus);
    }


    @Override
    public int getItemCount() {
        return courseList.size();
    }

}
