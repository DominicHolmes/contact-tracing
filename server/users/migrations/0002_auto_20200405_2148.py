# Generated by Django 3.0.5 on 2020-04-05 21:48

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("users", "0001_initial"),
    ]

    operations = [
        migrations.AlterField(
            model_name="device",
            name="android_id",
            field=models.TextField(blank=True, null=True),
        ),
        migrations.AlterField(
            model_name="device",
            name="apple_id",
            field=models.TextField(blank=True, null=True),
        ),
    ]
