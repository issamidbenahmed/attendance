from rest_framework import serializers
from .models import User
from .models import Attendance, Module, Student
import json

class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ['id', 'name', 'email', 'role']


class StudentSerializer(serializers.ModelSerializer):
    class Meta:
        model = Student
        fields = ['apogee_code', 'last_name', 'first_name', 'email']


class ModuleSerializer(serializers.ModelSerializer):
    class Meta:
        model = Module
        fields = ['name']


class AttendanceSerializer(serializers.ModelSerializer):
    studentName = serializers.SerializerMethodField()
    module = serializers.CharField(source='module.name')

    class Meta:
        model = Attendance
        fields = ['id', 'studentName', 'module', 'timestamp']

    def get_studentName(self, obj):
        return f"{obj.student.first_name} {obj.student.last_name}"



class ScanRequestSerializer(serializers.Serializer):
    qr_data = serializers.CharField()

    def validate_qr_data(self, value):
        try:
            # Parse the JSON-like structure from QR
            data = json.loads(value)
            required_fields = ['Code Apogée', 'Nom - Prénom', 'Email', 'Module']
            if not all(field in data for field in required_fields):
                raise serializers.ValidationError("QR data missing required fields")
            return data
        except json.JSONDecodeError:
            raise serializers.ValidationError("Invalid QR data format")