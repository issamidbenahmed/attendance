# Generated by Django 5.2 on 2025-04-13 21:47

import django.db.models.deletion
import django.utils.timezone
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('api', '0002_user_groups_user_is_active_user_is_staff_and_more'),
    ]

    operations = [
        migrations.CreateModel(
            name='Module',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('name', models.CharField(max_length=100)),
            ],
        ),
        migrations.CreateModel(
            name='Student',
            fields=[
                ('apogee_code', models.CharField(max_length=20, primary_key=True, serialize=False, unique=True)),
                ('last_name', models.CharField(max_length=100)),
                ('first_name', models.CharField(max_length=100)),
                ('email', models.EmailField(max_length=254)),
            ],
        ),
        migrations.AlterField(
            model_name='attendance',
            name='timestamp',
            field=models.DateTimeField(default=django.utils.timezone.now),
        ),
        migrations.AddField(
            model_name='attendance',
            name='module',
            field=models.ForeignKey(default=1, on_delete=django.db.models.deletion.CASCADE, to='api.module'),
            preserve_default=False,
        ),
        migrations.AddField(
            model_name='attendance',
            name='student',
            field=models.ForeignKey(default=1, on_delete=django.db.models.deletion.CASCADE, to='api.student'),
            preserve_default=False,
        ),
        migrations.AlterUniqueTogether(
            name='attendance',
            unique_together={('student', 'module', 'timestamp')},
        ),
        migrations.RemoveField(
            model_name='attendance',
            name='status',
        ),
        migrations.RemoveField(
            model_name='attendance',
            name='user',
        ),
    ]
