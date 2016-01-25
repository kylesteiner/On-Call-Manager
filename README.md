On-Call Manager Plugin
=================================

Below is an example of a configured entry for this resource in the config.yaml file "resources" section.

```
resources:
  - name: PagerDuty
    className: com.labs2160.oncall.ctr.PagerDutyProvider
    configuration:
      apiUri: "https://batman.pagerduty.com/api/v1"
      apiKey: "SecretKey"
      scheduleID: "SecretID"

  - name: OnCallDB
    className: com.labs2160.oncall.ctr.DatabaseProvider
    configuration:
      DBDir: "/tmp/oncallDB/"
```

Here are recommended workflows that should be added to the config.yaml file "workflows" sections.

```
workflows:
  - name: PagerDuty List User Totals
    alias: pd-list-totals
    action:
      className: com.labs2160.oncall.ctr.actions.ListTotalsAction

  - name: PagerDuty Add Override
    alias: pd-add-override
    action:
      className: com.labs2160.oncall.ctr.actions.AddOverrideAction

  - name: PagerDuty Set User Total
    alias: pd-set-total
    action:
      className: com.labs2160.oncall.ctr.actions.SetUserTotalAction
```
