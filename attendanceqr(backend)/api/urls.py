from django.urls import path
from .views import LoginView
from .views import ScanAttendanceView, AttendanceListView



urlpatterns = [
    path('login/', LoginView.as_view(), name='login'),
    path('scan/', ScanAttendanceView.as_view(), name='scan-attendance'),
    path('attendances/', AttendanceListView.as_view(), name='attendance-list'),
]
