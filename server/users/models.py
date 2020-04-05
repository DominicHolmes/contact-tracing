from django.db import models
from django.utils import timezone


class Device(models.Model):
    id = models.BigAutoField(primary_key=True)
    device_id = models.TextField()

    apple_id = models.TextField(null=True, blank=True)
    android_id = models.TextField(null=True, blank=True)

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)


class Contact(models.Model):
    self_device = models.ForeignKey(
        Device, on_delete=models.CASCADE, related_name="self_logged"
    )
    other_device = models.ForeignKey(
        Device, on_delete=models.CASCADE, related_name="others_logged"
    )
    timestamp = models.DateTimeField(default=timezone.now)
