package com.uhsl.flowmessage.flowmessagev2;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.uhsl.flowmessage.flowmessagev2.utils.DataSetChangedCallbackAdapter;

import java.util.List;

/**
 * Created by Marcus on 19/02/2016.
 */
public class ThreeLineOptionalHiddenArrayAdapter extends ArrayAdapter<String[]> {

    private final Activity context;
    private List<String[]> values;
    private final boolean hiddenRow1;
    private final boolean hiddenRow2;
    private final boolean hiddenRow3;

    // Holds the state of the view
    static class ViewHolder {
        public TextView lineOne;
        public TextView lineTwo;
        public TextView lineThree;
        public boolean active;
    }


    /**
     * Constructor
     *
     * @param context The current context
     * @param values The list of values the adapter uses to be represented in the ListView
     * @param hiddenRow1 Is row 1 hidden?
     * @param hiddenRow2 Is row 2 hidden?
     * @param hiddenRow3 Is row 3 hidden?
     */
    public ThreeLineOptionalHiddenArrayAdapter(Activity context, List<String[]> values,
                                               boolean hiddenRow1, boolean hiddenRow2, boolean hiddenRow3) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
        this.hiddenRow1 = hiddenRow1;
        this.hiddenRow2 = hiddenRow2;
        this.hiddenRow3 = hiddenRow3;
    }

    /**
     * Get the item position based on the data in any combination of the three lines.
     *
     * @param l1 Data in line one, can be null.
     * @param l2 Data in line two, can be null.
     * @param l3 Data in line three, can be null.
     * @return The item position
     */
    public int getItemPosition(String l1, String l2, String l3) {
        if (l1 != null || l2 !=null || l3 != null) {
            for (int pos = 0; pos < values.size(); pos++) {
                String[] item = values.get(pos);

                if ((l1 == null || l1.equals(item[0])) &&
                        (l2 == null || l2.equals(item[1])) &&
                        (l3 == null || l3.equals(item[2]))) {
                    return pos;
                }
            }
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;

        // inflate if not already displayed
        if (rowView == null) {
            LayoutInflater inflater = context.getLayoutInflater();

            rowView = inflater.inflate(R.layout.three_line_listview_row, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.lineOne = (TextView) rowView.findViewById(R.id.three_line_listView_row_first_line);
            viewHolder.lineTwo = (TextView) rowView.findViewById(R.id.three_line_listView_row_second_line);
            viewHolder.lineThree = (TextView) rowView.findViewById(R.id.three_line_listView_row_third_line);
            rowView.setTag(viewHolder);
        }

        // set up the ViewHolder
        ViewHolder viewHolder = (ViewHolder) rowView.getTag();
        viewHolder.lineOne.setText(values.get(position)[0]);
        viewHolder.lineTwo.setText(values.get(position)[1]);
        viewHolder.lineThree.setText(values.get(position)[2]);

        if (hiddenRow1)
            viewHolder.lineOne.setVisibility(View.GONE);
        if (hiddenRow2)
            viewHolder.lineTwo.setVisibility(View.GONE);
        if (hiddenRow3)
            viewHolder.lineThree.setVisibility(View.GONE);


        return rowView;
    }

    public void importCollection(List<String[]> values) {
        this.values = values;
        notifyDataSetChanged();
    }
}
