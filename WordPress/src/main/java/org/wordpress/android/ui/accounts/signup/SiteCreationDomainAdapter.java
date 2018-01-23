package org.wordpress.android.ui.accounts.signup;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse;

import java.util.List;

public class SiteCreationDomainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_INPUT = 1;
    private static final int VIEW_TYPE_ITEM = 2;

    public interface OnDomainKeywordsListener {
        void onChange(String keywords);
    }

    private boolean mIsLoading;
    private List<DomainSuggestionResponse> mSuggestions;
    private SiteCreationListener mSiteCreationListener;
    private OnDomainKeywordsListener mOnDomainKeywordsListener;

    private DomainSuggestionResponse mSelectedDomain;

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public final View progress;
        public final TextView label;

        HeaderViewHolder(View itemView) {
            super(itemView);
            this.progress = itemView.findViewById(R.id.progress_container);
            this.label = (TextView) itemView.findViewById(R.id.progress_label);
        }
    }

    public static class InputViewHolder extends RecyclerView.ViewHolder {
        public final EditText input;

        public InputViewHolder(View itemView) {
            super(itemView);
            this.input = itemView.findViewById(R.id.input);
        }
    }

    public static class DomainViewHolder extends RecyclerView.ViewHolder {
        public final RadioButton radioButton;

        public DomainViewHolder(View itemView) {
            super(itemView);
            radioButton = (RadioButton) itemView;
        }
    }

    public SiteCreationDomainAdapter(Context context, SiteCreationListener siteCreationListener,
            OnDomainKeywordsListener onDomainKeywordsListener) {
        super();
        ((WordPress) context.getApplicationContext()).component().inject(this);

        mSiteCreationListener = siteCreationListener;
        mOnDomainKeywordsListener = onDomainKeywordsListener;
    }

    public void setData(boolean isLoading, List<DomainSuggestionResponse> suggestions) {
        mIsLoading = isLoading;
        mSuggestions = suggestions;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.site_creation_domain_header,
                    parent, false);
            return new HeaderViewHolder(itemView);
        } else if (viewType == VIEW_TYPE_INPUT) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.site_creation_domain_input, parent,
                    false);
            return new InputViewHolder(itemView);
        } else {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.site_creation_domain_item, parent,
                    false);
            return new DomainViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);

        if (viewType == VIEW_TYPE_HEADER) {
//            final HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
        } else if (viewType == VIEW_TYPE_INPUT) {
            final InputViewHolder inputViewHolder = (InputViewHolder) holder;
            if (inputViewHolder.input.getTag() == null) {
                inputViewHolder.input.setTag(Boolean.TRUE);
                inputViewHolder.input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        mOnDomainKeywordsListener.onChange(editable.toString());
                    }
                });
            }
        } else {
            final DomainSuggestionResponse suggestion = getItem(position);
            final DomainViewHolder domainViewHolder = (DomainViewHolder) holder;
            domainViewHolder.radioButton.setText(suggestion.domain_name);
            domainViewHolder.radioButton.setChecked(suggestion.equals(mSelectedDomain));

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!suggestion.equals(mSelectedDomain)) {
                        mSelectedDomain = suggestion;
                        notifyDataSetChanged();
                    }

//                    mSiteCreationListener.withDomain(suggestion.domain_name);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return 2 + (mSuggestions == null ? 0 : mSuggestions.size());
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        } else if (position == 1) {
            return VIEW_TYPE_INPUT;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    private DomainSuggestionResponse getItem(int position) {
        return mSuggestions.get(position - 2);
    }
}