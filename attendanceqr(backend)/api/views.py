from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework_simplejwt.tokens import RefreshToken

from .models import User, Attendance
from .serializers import UserSerializer, AttendanceSerializer
from rest_framework.authentication import BasicAuthentication
from django.contrib.auth import authenticate
from rest_framework.views import APIView
from .serializers import UserSerializer
from .models import User
import logging
from rest_framework.response import Response
from rest_framework import status, permissions
from .models import Attendance, Module, Student
from .serializers import AttendanceSerializer, ScanRequestSerializer
from django.utils import timezone
import json

class LoginView(APIView):
    def post(self, request):
        email = request.data.get('email')
        password = request.data.get('password')

        print(f"Received email: {email}, password: {password}")

        if not email or not password:
            return Response({"error": "Email and password are required"}, status=400)

        user = authenticate(request, username=email, password=password)
        if user is not None:
            refresh = RefreshToken.for_user(user)

            return Response({
                'token': str(refresh.access_token),
                'refresh': str(refresh),
                'user': UserSerializer(user).data
            })
        else:
            return Response({'error': 'Invalid credentials'}, status=400)


logger = logging.getLogger(__name__)


class ScanAttendanceView(APIView):
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request):
        print(f"User authentifié: {request.user}")  # Debug
        print(f"Headers reçus: {request.headers}")
        serializer = ScanRequestSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

        qr_data = serializer.validated_data['qr_data']

        try:
            # Extract student information
            apogee_code = qr_data['Code Apogée']
            last_name, first_name = qr_data['Nom - Prénom'].split(' - ', 1)
            email = qr_data['Email']
            module_name = qr_data['Module']

            # Get or create student
            student, _ = Student.objects.get_or_create(
                apogee_code=apogee_code,
                defaults={
                    'last_name': last_name,
                    'first_name': first_name,
                    'email': email
                }
            )

            # Get or create module
            module, _ = Module.objects.get_or_create(name=module_name)

            # Create attendance record
            attendance = Attendance.objects.create(
                student=student,
                module=module
            )

            return Response({
                "status": "success",
                "attendanceId": attendance.id,
                "studentName": f"{first_name} {last_name}",
                "module": module_name,
                "timestamp": attendance.timestamp.strftime("%Y-%m-%d %H:%M:%S")
            }, status=status.HTTP_201_CREATED)

        except Exception as e:
            return Response(
                {"error": str(e)},
                status=status.HTTP_400_BAD_REQUEST
            )


class AttendanceListView(APIView):
    # 🔁 Modifier ceci :
    permission_classes = [permissions.AllowAny]

    def get(self, request):
        attendance = Attendance.objects.all()
        serializer = AttendanceSerializer(attendance.order_by('-timestamp'), many=True)
        return Response(serializer.data)