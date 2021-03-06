package tech.tensordash.tensordash.view.ui;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import tech.tensordash.tensordash.R;
import tech.tensordash.tensordash.service.model.Project;
import tech.tensordash.tensordash.service.model.ProjectParams;
import tech.tensordash.tensordash.viewmodel.FirebaseDatabaseViewModel;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class ProjectDescriptionActivity extends AppCompatActivity {

    private FirebaseDatabaseViewModel firebaseDatabaseViewModel;
    private TextView projectNameTextView;
    private TextView epochTextView;
    private TextView accuracyTextView;
    private TextView lossTextView;
    private TextView validationAccuracyTextView;
    private TextView validationLossTextView;
    private LineChart lineChartLoss;
    private LineChart lineChartAccuracy;
    private LineChart lineChartValidationLoss;
    private LineChart lineChartValidationAccuracy;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean isAccuracyPresent = true;
    private boolean isValidationLossPresent = true;
    private boolean isValidationAccuracyPresent = true;

    private Project thisProject;

    public static final int DELETE_PROJECT = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_description);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle("Project Details");

        projectNameTextView = findViewById(R.id.project_name_description_textview);
        epochTextView = findViewById(R.id.epoch_project_description_textview);
        accuracyTextView = findViewById(R.id.accuracy_project_description_textview);
        lossTextView = findViewById(R.id.loss_project_description_textview);
        validationAccuracyTextView = findViewById(R.id.validation_accuracy_project_description_textview);
        validationLossTextView = findViewById(R.id.validation_loss_project_description_textview);
        lineChartLoss = findViewById(R.id.chart_view_loss);
        lineChartAccuracy = findViewById(R.id.chart_view_accuracy);
        lineChartValidationLoss = findViewById(R.id.chart_view_validation_loss);
        lineChartValidationAccuracy = findViewById(R.id.chart_view_validation_accuracy);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout_description);

        loadActivity();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            firebaseDatabaseViewModel.refreshProjectList(swipeRefreshLayout);
            loadActivity();
        });


    }

    public void loadActivity() {

        firebaseDatabaseViewModel = ViewModelProviders.of(ProjectDescriptionActivity.this).get(FirebaseDatabaseViewModel.class);
        String projectName = getIntent().getStringExtra("project_name");
        firebaseDatabaseViewModel.getAllProjects().observe(this, projects -> {
            for (Project project : projects) {
                if (projectName.equals(project.getProjectName())) {
                    thisProject = project;
                    setValues();
                    break;
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setValues() {
        projectNameTextView.setText(thisProject.getProjectName());
        epochTextView.setText(String.valueOf(thisProject.getLatestEpoch()));
        accuracyTextView.setText(String.valueOf(thisProject.getLatestAccuracy()));
        lossTextView.setText(String.valueOf(thisProject.getLatestLoss()));
        validationAccuracyTextView.setText(String.valueOf(thisProject.getLatestValidationAccuracy()));
        validationLossTextView.setText(String.valueOf(thisProject.getLatestValidationLoss()));
        setUpChart(thisProject.getProjectParamsList());
    }

    private void setUpChart(List<ProjectParams> projectParamsList) {

        final int[] colors = new int[]{
                ColorTemplate.VORDIPLOM_COLORS[0],
                ColorTemplate.VORDIPLOM_COLORS[1],
                ColorTemplate.VORDIPLOM_COLORS[2],
                ColorTemplate.VORDIPLOM_COLORS[3],
        };

        ArrayList<Entry> lossEntries = new ArrayList<>();
        ArrayList<Entry> accuracyEntries = new ArrayList<>();
        ArrayList<Entry> validationLossEntries = new ArrayList<>();
        ArrayList<Entry> validationAccuracyEntries = new ArrayList<>();
        for (ProjectParams projectParams : projectParamsList) {
            if (projectParams.getEpoch() == 0 && projectParamsList.size() > 1) {
                continue;
            }
            lossEntries.add(new Entry(projectParams.getEpoch(), (float) projectParams.getLoss()));
            accuracyEntries.add(new Entry(projectParams.getEpoch(), (float) projectParams.getAccuracy()));
            validationLossEntries.add(new Entry(projectParams.getEpoch(), (float) projectParams.getValidationLoss()));
            validationAccuracyEntries.add(new Entry(projectParams.getEpoch(), (float) projectParams.getValidationAccuracy()));
        }
        shouldChartExist(projectParamsList);
        createChart(lossEntries, "Loss", colors[0], lineChartLoss);

        if (isAccuracyPresent) {
            createChart(accuracyEntries, "Accuracy", colors[1], lineChartAccuracy);
        } else {
            lineChartAccuracy.setVisibility(View.GONE);
            findViewById(R.id.chart_textView_accuracy_description).setVisibility(View.GONE);
        }

        if (isValidationAccuracyPresent) {
            createChart(validationAccuracyEntries, "Validation Accuracy", colors[2], lineChartValidationAccuracy);
        } else {
            lineChartValidationAccuracy.setVisibility(View.GONE);
            findViewById(R.id.chart_textView_validation_accuracy_description).setVisibility(View.GONE);
        }

        if (isValidationLossPresent) {
            createChart(validationLossEntries, "Validation Loss", colors[3], lineChartValidationLoss);
        } else {
            lineChartValidationLoss.setVisibility(View.GONE);
            findViewById(R.id.chart_textView_validation_loss_description).setVisibility(View.GONE);
        }


    }

    private void createChart(ArrayList<Entry> entries, String label, int color, LineChart lineChart) {
        float textSize = 9;

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setValueTextColor(color);
        dataSet.setValueTextSize(textSize);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
        xAxis.setTextColor(Color.WHITE);

        YAxis yAxisRight = lineChart.getAxisRight();
        yAxisRight.setEnabled(false);

        YAxis yAxisLeft = lineChart.getAxisLeft();
        yAxisLeft.setTextColor(Color.WHITE);
        yAxisLeft.setGranularity(1f);

        LineData data = new LineData(dataSet);

        lineChart.getLegend().setTextColor(Color.WHITE);
        lineChart.getDescription().setEnabled(false);
        lineChart.setData(data);
        lineChart.invalidate();
    }

    private void shouldChartExist(List<ProjectParams> projectParamsArrayList) {
        float accuracySum = 0, valLossSum = 0, valAccSum = 0;
        for (ProjectParams projectParams : projectParamsArrayList) {
            accuracySum += projectParams.getAccuracy();
            valLossSum += projectParams.getValidationLoss();
            valAccSum += projectParams.getValidationAccuracy();
        }
        if (accuracySum == 0) {
            isAccuracyPresent = false;
        }
        if (valLossSum == 0) {
            isValidationLossPresent = false;
        }
        if (valAccSum == 0) {
            isValidationAccuracyPresent = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_project_description, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_project_delete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ProjectDescriptionActivity.this);
            builder.setTitle("Sign out?")
                    .setMessage("Do you want to delete \"" + thisProject.getProjectName() + "\" ?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        deleteProject(thisProject.getProjectName());
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                    })
                    .create()
                    .show();
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteProject(String projectName){
        Intent intent = new Intent();
        intent.putExtra("delete_project", projectName);
        setResult(DELETE_PROJECT, intent);
        finish();
    }

}
