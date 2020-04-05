from django.http import JsonResponse
from django.shortcuts import render
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods

from .models import Device


@csrf_exempt
@require_http_methods(["POST"])
def register(request):
    device_id = request.POST.get("device_id")

    if not device_id:
        return JsonResponse(
            {"error": "Please pass a 'device_id' parameter to the server!"}
        )

    obj, _ = Device.objects.get_or_create(device_id=device_id)

    for id_field in ("apple_id", "android_id"):
        field = request.POST.get(id_field)
        if field:
            setattr(obj, id_field, field)
            obj.save()
            break
    else:
        return JsonResponse(
            {
                "error": "Please pass either 'apple_id' or 'android_id' for push notifications!"
            }
        )

    return JsonResponse({"id": obj.id})


@csrf_exempt
@require_http_methods(["POST"])
def contact(request):
    pass


@csrf_exempt
@require_http_methods(["POST"])
def alert(request):
    pass
