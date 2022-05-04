import unittest

from google.protobuf.timestamp_pb2 import Timestamp
from google.protobuf.json_format import MessageToJson, Parse

from apxprotobufs.Menage_pb2 import Command, Status, MenageResponse, CreateBody, DeleteBody, ScaleBody, ReportBody, ClusterMeta
from apxprotobufs.MessageMetadata_pb2 import XUUID
from apxprotobufs.LambdaMessages_pb2 import LambdaInitParams


class TestMenageMessages(unittest.TestCase):

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.algorithm_id = XUUID(type="B", uuid="8336a94b-017d-46b9-acfd-ee66f9de2ab0")
        self.logical_id = "StructuredSuspects:1.0.0/1.0.1-V23"
        self.environment = "stg"
        self.inbound_topic = "lambda-request-topic-" + self.environment
        self.outbound_topic = "lambda-return-topic-" + self.environment
        self.error_topic = "lambda-topic-error-" + self.environment
        self.dead_letter_topic = "lambda-topic-deadletter-" + self.environment
        self.repository = "snapshots"
        self.version = "2.3.7"

    def testMenageCreateMessage(self):
        timestamp = Timestamp()
        instance_type = "r5.xlarge"
        lambda_init_params = LambdaInitParams(algorithmXUUID=self.algorithm_id, inboundTopic=self.inbound_topic, outboundTopic=self.outbound_topic, \
                          errorTopic=self.error_topic, deadLetterTopic=self.dead_letter_topic, instanceCount=1)
        create_body = CreateBody(algorithmID=self.algorithm_id, environment=self.environment, instanceType=instance_type, \
                                 capacity=5, lambdaInitParams=lambda_init_params, repository=self.repository, version=self.version,
                                 autoscale=True, unittest=False)
        success_response = MenageResponse(code=200, status=Status.SUCCESS, message="some message", command=Command.CREATE, \
                                  messageDateTime=timestamp, createBody=create_body)

        success_serialized = MessageToJson(success_response, including_default_value_fields=True)
        print("Serialized create_response (success) is %s" % success_serialized)
        deserialized = Parse(success_serialized, MenageResponse(), ignore_unknown_fields=True)
        success_serialized2 = MessageToJson(deserialized, including_default_value_fields=True)
        print("Deserialized create_response (success) is %s" % success_serialized2)
        self.assertEqual(success_serialized, success_serialized2)

        failure_response = MenageResponse(code=400, status=Status.FAILURE, message="exception", command=Command.CREATE, \
                                          messageDateTime=timestamp, createBody=create_body)

        failure_serialized = MessageToJson(failure_response, including_default_value_fields=True)
        print("Serialized create_response (failure) is %s" % failure_serialized)
        deserialized2 = Parse(failure_serialized, MenageResponse(), ignore_unknown_fields=True)
        failure_serialized2 = MessageToJson(deserialized2, including_default_value_fields=True)
        print("Deserialized create_response (failure) is %s" % failure_serialized2)
        self.assertEqual(failure_serialized, failure_serialized2)

    def testMenageDeleteMessage(self):
        timestamp = Timestamp()
        instance_type = "r5.xlarge"
        lambda_init_params = LambdaInitParams(algorithmXUUID=self.algorithm_id, inboundTopic=self.inbound_topic, outboundTopic=self.outbound_topic, \
                          errorTopic=self.error_topic, deadLetterTopic=self.dead_letter_topic, instanceCount=1)
        delete_body = DeleteBody(algorithmID=self.algorithm_id, environment=self.environment, instanceType=instance_type, capacity=1, lambdaInitParams=lambda_init_params)
        success_response = MenageResponse(code=200, status=Status.SUCCESS, message="some message", command=Command.DELETE, \
                                          messageDateTime=timestamp, deleteBody=delete_body)
        success_serialized = MessageToJson(success_response, including_default_value_fields=True)
        print("Serialized delete_response (success) is %s" % success_serialized)
        deserialized = Parse(success_serialized, MenageResponse(), ignore_unknown_fields=True)
        success_serialized2 = MessageToJson(deserialized, including_default_value_fields=True)
        print("Deserialized delete_response (success) is %s" % success_serialized2)
        self.assertEqual(success_serialized, success_serialized2)

        failure_response = MenageResponse(code=400, status=Status.FAILURE, message="exception", command=Command.DELETE, \
                                                  messageDateTime=timestamp, deleteBody=delete_body)

        failure_serialized = MessageToJson(failure_response, including_default_value_fields=True)
        print("Serialized delete_response (failure) is %s" % failure_serialized)
        deserialized2 = Parse(failure_serialized, MenageResponse(), ignore_unknown_fields=True)
        failure_serialized2 = MessageToJson(deserialized2, including_default_value_fields=True)
        print("Deserialized delete_response (failure) is %s" % failure_serialized2)
        self.assertEqual(failure_serialized, failure_serialized2)

    def testMenageScaleOutMessage(self):
        timestamp = Timestamp()
        ips = ["10.1.32.81", "10.1.48.117", "10.1.48.150", "10.1.48.119", "10.1.32.10", "10.1.4.5"]
        scale_body = ScaleBody(algorithmID=self.algorithm_id, environment=self.environment, capacity=6, diff=3, ip=ips)
        success_response = MenageResponse(code=200, status=Status.SUCCESS, message="some message", command=Command.SCALE_OUT, \
                                          messageDateTime=timestamp, scaleBody=scale_body)

        success_serialized = MessageToJson(success_response, including_default_value_fields=True)
        print("Serialized scale_out_response (success) is %s" % success_serialized)
        deserialized = Parse(success_serialized, MenageResponse(), ignore_unknown_fields=True)
        success_serialized2 = MessageToJson(deserialized, including_default_value_fields=True)
        print("Deserialized scale_out_response (success) is %s" % success_serialized2)
        self.assertEqual(success_serialized, success_serialized2)

        failure_response = MenageResponse(code=400, status=Status.FAILURE, message="exception", command=Command.SCALE_OUT, \
                                          messageDateTime=timestamp, scaleBody=scale_body)

        failure_serialized = MessageToJson(failure_response, including_default_value_fields=True)
        print("Serialized scale_out_response (failure) is %s" % failure_serialized)
        deserialized2 = Parse(failure_serialized, MenageResponse(), ignore_unknown_fields=True)
        failure_serialized2 = MessageToJson(deserialized2, including_default_value_fields=True)
        print("Deserialized scale_out_response (failure) is %s" % failure_serialized2)
        self.assertEqual(failure_serialized, failure_serialized2)

    def testMenageScaleInMessage(self):
        timestamp = Timestamp()
        ips = ["10.1.32.81", "10.1.48.117", "10.1.48.150"]
        scale_body = ScaleBody(algorithmID=self.algorithm_id, environment=self.environment, capacity=3, diff=3, ip=ips)
        success_response = MenageResponse(code=200, status=Status.SUCCESS, message="some message", command=Command.SCALE_IN, \
                                          messageDateTime=timestamp, scaleBody=scale_body)

        success_serialized = MessageToJson(success_response, including_default_value_fields=True)
        print("Serialized scale_in_response (success) is %s" % success_serialized)
        deserialized = Parse(success_serialized, MenageResponse(), ignore_unknown_fields=True)
        success_serialized2 = MessageToJson(deserialized, including_default_value_fields=True)
        print("Deserialized scale_in_response (success) is %s" % success_serialized2)
        self.assertEqual(success_serialized, success_serialized2)

        ips = ["10.1.32.81", "10.1.48.117", "10.1.48.150", "10.1.48.119", "10.1.32.10", "10.1.4.5"]
        scale_body = ScaleBody(algorithmID=self.algorithm_id, environment=self.environment, capacity=6, diff=3, ip=ips)
        failure_response = MenageResponse(code=400, status=Status.FAILURE, message="exception", command=Command.SCALE_IN, \
                                          messageDateTime=timestamp, scaleBody=scale_body)

        failure_serialized = MessageToJson(failure_response, including_default_value_fields=True)
        print("Serialized scale_in_response (failure) is %s" % failure_serialized)
        deserialized2 = Parse(failure_serialized, MenageResponse(), ignore_unknown_fields=True)
        failure_serialized2 = MessageToJson(deserialized2, including_default_value_fields=True)
        print("Deserialized scale_in_response (failure) is %s" % failure_serialized2)
        self.assertEqual(failure_serialized, failure_serialized2)

    def testMenageReportMessage(self):
        timestamp = Timestamp()
        algorithm_id = XUUID(type="B", uuid="45ccf74c-aeff-46d5-ab20-47227f778ff5")
        logical_id = "DateIsAssumed:1.0.0/1.0.1-V23QA"
        cluster_meta = ClusterMeta(algorithmID=self.algorithm_id, logicalID=self.logical_id, environment=self.environment, \
                                   capacity=3, cluster="lambdaecc-stg-B_8336a94b-017d-46b9-acfd-ee66f9de2ab0", \
                                   ip=["10.1.32.81", "10.1.48.117", "10.1.48.150"], autoscale=True, unittest=False, \
                                   instanceType="m5.xlarge", algorithmType="PATIENT_SIGNAL")
        cluster_meta2 = ClusterMeta(algorithmID=algorithm_id, logicalID=logical_id, environment=self.environment, \
                                    capacity=3, cluster="lambdaecc-stg-B_45ccf74c-aeff-46d5-ab20-47227f778ff5", \
                                    ip=["10.1.48.119", "10.1.32.10", "10.1.4.5"], autoscale=False, unittest=True, \
                                    instanceType="m5.xlarge", algorithmType="PATIENT_SIGNAL")
        report_body = ReportBody()
        report_body.clusterMeta.extend([cluster_meta, cluster_meta2])

        success_response = MenageResponse(code=200, status=Status.SUCCESS, message="some message", command=Command.REPORT, \
                                          messageDateTime=timestamp, reportBody=report_body)

        success_serialized = MessageToJson(success_response, including_default_value_fields=True)
        print("Serialized report_response (success) is %s" % success_serialized)
        deserialized = Parse(success_serialized, MenageResponse(), ignore_unknown_fields=False)
        success_serialized2 = MessageToJson(deserialized, including_default_value_fields=True)
        print("Deserialized report_response (success) is %s" % success_serialized2)
        self.assertEqual(success_serialized, success_serialized2)

        report_body = ReportBody()

        failure_response = MenageResponse(code=400, status=Status.FAILURE, message="exception", command=Command.REPORT, \
                                          messageDateTime=timestamp, reportBody=report_body)
        failure_serialized = MessageToJson(failure_response, including_default_value_fields=True)
        print("Serialized report_response (failure) is %s" % failure_serialized)
        deserialized2 = Parse(failure_serialized, MenageResponse(), ignore_unknown_fields=False)
        failure_serialized2 = MessageToJson(deserialized2, including_default_value_fields=True)
        print("Deserialized report_response (failure) is %s" % failure_serialized2)
        self.assertEqual(failure_serialized, failure_serialized2)

if __name__ == '__main__':
    unittest.main()