from django.db import models


class Device(models.Model):
    id = models.BigAutoField(primary_key=True)
    device_id = models.TextField()

    apple_id = models.TextField(null=True, blank=True)
    android_id = models.TextField(null=True, blank=True)

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
