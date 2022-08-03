package org.torproject.android.tensor;

import android.app.Activity
import android.content.Context
import android.util.Log
import com.maxkeppeler.sheets.info.InfoSheet
import org.cleaninsights.sdk.*
import java.io.IOException
import org.torproject.android.R


open class CleanInsightsManager {

    val CI_CAMPAIGN = "connection-labeling"

    private var mMeasure: CleanInsights? = null
    private var mHasConsent = true

    fun initMeasurement(context : Context) {
        if (mMeasure == null) {
            // Instantiate with configuration and directory to write store to, best in an `Application` subclass.
            try {

                mMeasure = CleanInsights(
                    context.assets.open("cleaninsights.json").reader().readText(),
                    context.filesDir
                )

                mHasConsent = mMeasure!!.isCampaignCurrentlyGranted(CI_CAMPAIGN)


            } catch (e: IOException) {
                e.printStackTrace()
            }
        }


    }


    private fun getConsent(context: Activity, campaignId: String?, labeledData: String) {

        mMeasure?.testServer {
            Log.d("Clean Insights", "test server: " + it.toString())
        }

        var success = mMeasure!!.requestConsent(campaignId!!, object : JavaConsentRequestUi {
            override fun show(
                s: String,
                campaign: Campaign,
                consentRequestUiCompletionHandler: ConsentRequestUiCompletionHandler
            ) {

                InfoSheet().show(context) {
                    title(context.getString(R.string.ci_title))
                    content(context.getString(R.string.clean_insight_consent_prompt))
                    onNegative(context.getString(R.string.ci_negative)) {
                        // Handle event
                        consentRequestUiCompletionHandler.completed(false)
                    }
                    onPositive(context.getString(R.string.ci_confirm)) {
                        // Handle event
                        mHasConsent = true
                        consentRequestUiCompletionHandler.completed(true)
                        mMeasure!!.grant(campaignId)

                       // addMeasurementAccuracy(context, viewId, isAccurate)
                        addLabeledData(context, labeledData)
                    }
                }

            }

            override fun show(
                feature: Feature,
                consentRequestUiCompletionHandler: ConsentRequestUiCompletionHandler
            ) {

            }


        })

        return success
    }

    fun measureView(view: String, campaignId: String?) {
        if (mHasConsent) {
            val alPath = ArrayList<String>()
            alPath.add(view)
            mMeasure?.measureVisit(alPath, campaignId!!)
        }
    }

    fun measureEvent(key: String?, value: String?, campaignId: String?) {
        if (mHasConsent) {
            mMeasure?.measureEvent(key!!, value!!, campaignId!!)

        }
    }

    fun persistMeasurements () {
        mMeasure?.persist()
    }

    /**
    fun showInfo (context : Activity) {


    }**/

    fun addLabeledData (activity : Activity, labeledData: String) {

        if (!mHasConsent) {
            val result = getConsent(activity, CI_CAMPAIGN, labeledData)

        }
        else {
            measureEvent("labeled_data", labeledData, CI_CAMPAIGN)
        }

    }


    fun showConfirm (activity : Activity, labeledData: String) {

        if (!mHasConsent) {
            val result = getConsent(activity, CI_CAMPAIGN, labeledData)

        }
        else {

            //measureView("survey:" + viewId, CI_CAMPAIGN)

            InfoSheet().show(activity) {
                title(activity.getString(R.string.measurement_survey_title))
                content(activity.getString(R.string.measurement_survey_content))
                onNegative(activity.getString(R.string.no_thx)) {

                    mMeasure?.persist()
                }
                onPositive(activity.getString(R.string.sheets_ok)) {
                    addLabeledData(activity, labeledData);
                    mMeasure?.persist()
                }
            }

        }
    }


}
