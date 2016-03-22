package com.uhsl.flowmessage.flowmessagev2.utils;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.List;

/**
 * Created by Marcus on 17/03/2016.
 */
public class DataSetChangedCallbackAdapter<TYPE> extends ArrayAdapter<TYPE> {

    private Runnable dataSetChangedCallback;

    /**
     * Constructor
     *
     * @param context  The current context.
     * @param resource The resource ID for a layout file containing a TextView to use when
     *                 instantiating views.
     * @param objects  The objects to represent in the ListView.
     */
    public DataSetChangedCallbackAdapter(Context context, int resource, List<TYPE> objects, Runnable callback) {
        super(context, resource, objects);

        dataSetChangedCallback = callback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        if (dataSetChangedCallback != null)
            dataSetChangedCallback.run();
    }
}
