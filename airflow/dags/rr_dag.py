import json
from datetime import datetime, timedelta

from airflow.decorators import dag, task

@dag(schedule_interval="1-59 * * * *", start_date=datetime(2021, 1, 1), catchup=False, tags=["example"])
def tutorial_taskflow_api_etl():
    """
    ### Get Reference Rates from New York Fed
    Use open APIs from New York Fed to fetch all reference rates,
    including secured and unsecured ones.
    """
    @task()
    def extract():
        """
        #### Extract task
        A simple Extract task.
        """
        data_string = '{"1001": 301.27, "1002": 433.21, "1003": 502.22}'

        order_data_dict = json.loads(data_string)
        return order_data_dict
    
    @task(multiple_outputs=True)
    def transform(order_data_dict: dict):
        """
        #### Transform task
        A simple Transform task
        """
        total_order_value = 0
        for value in order_data_dict.values():
            total_order_value += value

        return {"total_order_value" : total_order_value}
    
    @task()
    def load(total_order_value: float):
        """
        #### Load task
        A simple Load task.
        """
        print(f"Total order value is: {total_order_value:.2f}")
    
    # the invocation itself automatically generates the deps
    order_data = extract()
    order_summary = transform(order_data)
    load(order_summary["total_order_value"])

tutorial_etl_dag = tutorial_taskflow_api_etl()