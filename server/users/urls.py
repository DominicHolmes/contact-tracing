from django.urls import path

from .views import alert, contact, register

urlpatterns = (
    path(r"register/", register),
    path(r"contact/", contact),
    path(r"alert/", alert),
)
